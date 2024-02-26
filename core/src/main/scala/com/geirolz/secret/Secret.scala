package com.geirolz.secret

import cats.{Eq, MonadThrow}
import com.geirolz.secret.Secret.*
import com.geirolz.secret.internal.{SecretApi, SecretCompanionApi}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.*

import scala.util.Try

/** Memory-safe and type-safe secret value of type `T`.
  *
  * `Secret` does the best to avoid leaking information in memory and in the code BUT an attack is possible and I don't
  * give any certainties or guarantees about security using this class, you use it at your own risk. Code is open
  * source, you can check the implementation and take your decision consciously. I'll do my best to improve the security
  * and documentation of this class.
  *
  * <b>Obfuscation</b>
  *
  * The value is obfuscated when creating the `Secret` instance using the given `SecretStrategy` which, by default,
  * transform the value into a xor-ed `ByteBuffer` witch store bytes outside the JVM using direct memory access.
  *
  * The obfuscated value is de-obfuscated using the given `SecretStrategy` instance every time the method `use` is
  * invoked which returns the original value converting bytes back to `T` re-apply the xor.
  *
  * <b>API and Type safety</b>
  *
  * While obfuscating the value prevents or at least makes it harder to read the value from memory, Secret class API are
  * designed to avoid leaking information in other ways. Preventing developers to improperly use the secret value (
  * logging, etc...).
  *
  * Example
  * {{{
  *   val secretString: Secret[String]  = Secret("my_password")
  *   val database: F[Database]         = secretString.use(password => initDb(password))
  * }}}
  */
trait Secret[T] extends SecretApi[T]:

  import cats.syntax.all.*

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
    */
  def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U]

  /** map the value using the specified function.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    *
    * * This current secret will NOT be destroyed when the new secret is destroyed.
    *
    * Example:
    * {{{
    *  val secretString: Secret[String]
    *  val secretInt: Secret[Int] = secretString.map(_.toInt)
    *  secretInt.destroy()
    *  secretString.isDestroyed // false
    * }}}
    */
  final def map[U: SecretStrategy](f: T => U)(using Hasher): Secret[U] =
    transform(_.euse(f.andThen(Secret[U](_))))

  /** flat map the value using the specified function.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    *
    * This current secret will NOT be destroyed when the new secret is destroyed.
    *
    * Example:
    * {{{
    *  val secretString: Secret[String]
    *  val secretInt: Secret[Int] = secretString.flatMap(s => Secret(s.toInt))
    *  secretInt.destroy()
    *  secretString.isDestroyed // false
    * }}}
    */
  final def flatMap[U: SecretStrategy](f: T => Secret[U])(using Hasher): Secret[U] =
    transform(_.euse(f))

  /** Duplicate the secret without destroying it.
    *
    * If this was destroyed the duplicated will also be destroyed.
    */
  def duplicate: Secret[T]

  /** [[OneShotSecret]] version of this secret */
  def asOneShot: OneShotSecret[T]

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `SecretDestroyed` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `SecretDestroyed` exception.
    */
  inline def use[F[_]: MonadSecretError, U](f: T => U): F[U] =
    evalUse[F, U](f.andThen(_.pure[F]))

  /** Alias for `use` with `Either[Throwable, *]` */
  inline def euse[U](f: T => U): Either[SecretDestroyed, U] =
    use[Either[SecretDestroyed, *], U](f)

  /** Safely compare this secret with the provided `Secret`.
    *
    * @return
    *   `true` if the secrets are equal, `false` if they are not equal or if one of the secret is destroyed
    */
  inline def isValueEquals(that: Secret[T])(using Eq[T]): Boolean =
    evalUse[Try, Boolean](thisValue => that.use[Try, Boolean](_ === thisValue)).getOrElse(false)

  // Private API
  /** Avoid this method if possible. Unsafely apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `SecretDestroyed` exception.
    *
    * Throws `SecretNoLongerValid` if the secret has been already destroyed
    */
  @throws[SecretDestroyed]("if the secret has been already destroyed")
  inline def unsafeUse[U](f: T => U): U =
    use[Either[SecretDestroyed, *], U](f).fold(throw _, identity)

object Secret extends SecretCompanionApi[Secret]:

  import cats.syntax.all.given
  export DeferredSecret.apply as defer
  export DeferredSecret.fromEnv as deferFromEnv

  /** Create a destroyed secret */
  override def destroyed[T](location: Location = Location.unknown): Secret[T] =
    new Secret[T]:
      override def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] = SecretDestroyed(location).raiseError[F, U]
      override def destroy()(using location: Location): Unit              = ()
      override def destructionLocation: Option[Location]                  = Some(location)
      override def isDestroyed: Boolean                                   = true
      override def hashed: String                                         = destroyedTag
      override def duplicate: Secret[T]                                   = this
      override def asOneShot: OneShotSecret[T]                            = OneShotSecret.destroyed(location)

  /** Create a secret from the given value */
  override def apply[T](value: => T, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): Secret[T] =
    val underlying: OneShotSecret[T] = OneShotSecret[T](value, collectDestructionLocation)
    new Secret[T]:
      override def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] = underlying.evalUse(f)
      override def destroy()(using location: Location): Unit              = underlying.destroy()
      override def destructionLocation: Option[Location]                  = underlying.destructionLocation
      override def isDestroyed: Boolean                                   = underlying.isDestroyed
      override def hashed: String                                         = underlying.hashed
      override def duplicate: Secret[T]                                   = map(identity)
      override def asOneShot: OneShotSecret[T]                            = underlying
