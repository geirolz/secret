package com.geirolz.secret.internal

import cats.Eval
import cats.syntax.all.*
import com.geirolz.secret.*
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.{BytesUtils, Hasher, KeyValueBuffer, Location}
import java.nio.ByteBuffer

private[secret] trait Vault[T] extends AutoCloseable:
  def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U]
  def destroy()(using Location): Unit
  def destructionLocation: Option[Location]
  def isDestroyed: Boolean
  def hashed: String
  inline def isHashedEquals(that: Secret[T]): Boolean = hashed == that.hashed
  inline final override def equals(obj: Any): Boolean = false
  inline final override def close(): Unit             = destroy()
  override final val toString: String                 = vaultTag

private[secret] object Vault:

  def destroyed[T](location: Location = Location.unknown): Vault[T] =
    new Vault[T]:
      override final def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
        SecretDestroyed(location).raiseError[F, U]
      override final def destroy()(using location: Location): Unit = ()
      override final def destructionLocation: Option[Location]     = None
      override final def isDestroyed: Boolean                      = true
      override final def hashed: String                            = destroyedTag

  def apply[T](value: => T, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): Vault[T] =

    var bufferTuple: KeyValueBuffer | Null = strategy.obfuscator(value)
    var hashedValue: Eval[ByteBuffer] | Null = Eval.later(
      hasher.hash(
        chars   = value.toString.getBytes,
        maxSize = Some(12)
      )
    )

    // do not use value inside the secret to avoid closure
    val vault = new Vault[T] {

      private var _destructionLocation: Option[Location] = None

      override final def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
        if (isDestroyed)
          SecretDestroyed(_destructionLocation.getOrElse(Location.unknown)).raiseError[F, U]
        else
          f(SecretStrategy[T].deObfuscator(bufferTuple))

      override final def destroy()(using location: Location): Unit =
        bufferTuple.destroy()
        bufferTuple = null
        hashedValue.map(BytesUtils.clearByteBuffer(_))
        hashedValue          = null
        _destructionLocation = if (collectDestructionLocation) Some(location) else None

      override final def destructionLocation: Option[Location] =
        Option(_destructionLocation).flatten

      override final def isDestroyed: Boolean =
        bufferTuple == null

      override final def hashed: String =
        if (isDestroyed)
          destroyedTag
        else
          BytesUtils.asString(hashedValue.value)
    }

    // clear the value when runtime shutdown
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run(): Unit = vault.destroy()
    })

    vault
