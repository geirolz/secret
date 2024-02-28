package com.geirolz.secret

import cats.syntax.all.*
import com.geirolz.secret.internal.{SecretApi, SecretCompanionApi, Vault}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.{Hasher, Location}

/** A `OneShotSecret` is a secret that can be used only once.
  *
  * After the first use, the secret is destroyed.
  *
  * @tparam T
  *   type of the secret
  */
trait OneShotSecret[T] extends SecretApi[T]
object OneShotSecret extends SecretCompanionApi[OneShotSecret]:

  /** Create a destroyed secret */
  override def destroyed[T](location: Location = Location.unknown): OneShotSecret[T] =
    new OneShotSecret[T]:
      override def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] = SecretDestroyed(location).raiseError[F, U]
      override def destroy()(using location: Location): Unit              = ()
      override def destructionLocation: Option[Location]                  = Some(location)
      override def isDestroyed: Boolean                                   = true
      override def hashed: String                                         = destroyedTag

  /** Create a new `OneShotSecret` with the specified value.
    *
    * @param value
    *   the value to obfuscate
    * @param collectDestructionLocation
    *   if `true` the location where the secret was destroyed will be collected
    * @return
    *   a new `OneShotSecret`
    */
  override def apply[T](value: => T, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): OneShotSecret[T] =
    val underlying: Vault[T] = Vault[T](value, collectDestructionLocation)
    new OneShotSecret[T]:
      override def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] = underlying.evalUse(f)
      override def destroy()(using location: Location): Unit              = underlying.destroy()
      override def destructionLocation: Option[Location]                  = underlying.destructionLocation
      override def isDestroyed: Boolean                                   = underlying.isDestroyed
      override def hashed: String                                         = underlying.hashed
