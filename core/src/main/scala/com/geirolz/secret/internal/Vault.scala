package com.geirolz.secret.internal

import cats.Eval
import cats.syntax.all.*
import com.geirolz.secret.*
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.transform.Hasher
import com.geirolz.secret.util.{BytesUtils, KeyValueBuffer, Location}

import java.nio.ByteBuffer

private[secret] trait Vault[T](
  val _recDestructionLocation: Boolean,
  val _hasher: Hasher
) extends AutoCloseable:
  def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U]
  def destroy()(using Location): Unit
  def destructionLocation: Option[Location]
  def isDestroyed: Boolean
  def hash: String
  inline def isHashEquals(that: Secret[T]): Boolean   = hash == that.hash
  inline final override def equals(obj: Any): Boolean = false
  inline final override def close(): Unit             = destroy()
  override final val toString: String                 = vaultTag

private[secret] object Vault:

  def destroyed[T](location: Location = Location.unknown): Vault[T] =
    new Vault[T](false, Hasher.default):
      override final def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
        SecretDestroyed(location).raiseError[F, U]
      override final def destroy()(using location: Location): Unit = ()
      override final def destructionLocation: Option[Location]     = None
      override final def isDestroyed: Boolean                      = true
      override final def hash: String                              = destroyedTag

  def apply[T](value: => T, recDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): Vault[T] =

    var bufferTuple: KeyValueBuffer | Null =
      strategy.obfuscator(value)

    var hashValue: Eval[ByteBuffer] | Null =
      Eval.later(hasher.hash(value.toString.getBytes))

    // do not use value inside the secret to avoid closure
    val vault = new Vault[T](recDestructionLocation, hasher) {

      private var _destructionLocation: Option[Location] = None

      override final def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
        if isDestroyed
        then SecretDestroyed(_destructionLocation.getOrElse(Location.unknown)).raiseError[F, U]
        else f(SecretStrategy[T].deObfuscator(bufferTuple))

      override final def destroy()(using location: Location): Unit =
        if !isDestroyed then
          bufferTuple.destroy()
          bufferTuple = null

          hashValue.map(BytesUtils.clearByteBuffer(_))
          hashValue = null

          _destructionLocation =
            if recDestructionLocation then Some(location)
            else None
        else ()

      override final def destructionLocation: Option[Location] =
        Option(_destructionLocation).flatten

      override final def isDestroyed: Boolean =
        bufferTuple == null

      override final def hash: String =
        if isDestroyed then destroyedTag
        else BytesUtils.asString(hashValue.value)
    }

    // clear the value when runtime shutdown
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = vault.destroy()
    })

    vault
