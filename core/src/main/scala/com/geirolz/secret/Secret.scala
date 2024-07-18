package com.geirolz.secret

import cats.{Eq, MonadThrow}
import com.geirolz.secret.Secret.*
import com.geirolz.secret.internal.{SecretApi, SecretCompanionApi, Vault}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.*

import scala.util.Try

/** Memory-safe and type-safe secret value of type `T`.
  *
  * A.K.A. `ReusableSecret`
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
abstract sealed class Secret[T] private (vault: Vault[T]) extends SecretApi[T](vault):

  import cats.syntax.all.*

  /** Duplicate the secret without destroying it.
    *
    * If this was destroyed the duplicated will also be destroyed.
    */
  def duplicate: Secret[T]

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
    */
  final def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
    vault.evalUse(f)

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

  /** [[Secret.OneShot]] version of this secret */
  final def asOneShot: Secret.OneShot[T] =
    Secret.oneShot.fromVault(vault)

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

object Secret extends SecretCompanionApi[Secret]:

  type OneShot[T] = OneShotSecret[T]
  final val oneShot = OneShotSecret

  type Deferred[F[_], T] = DeferredSecret[F, T]
  final val deferred = DeferredSecret

  /** Create a destroyed secret */
  override def destroyed[T](location: Location = Location.unknown): Secret[T] =
    new Secret[T](Vault.destroyed(location)):
      override def duplicate: Secret[T] = this

  /** Create a secret from the given value */
  override def apply[T](value: => T, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): Secret[T] =
    new Secret[T](Vault[T](value, collectDestructionLocation)):
      override def duplicate: Secret[T] = map(identity)
