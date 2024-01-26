package com.geirolz.secret

import cats.{Eq, Monoid, Show}
import com.geirolz.secret.Secret.*
import com.geirolz.secret.internal.Location
import com.geirolz.secret.strategy.SecretStrategy

import scala.util.Try
import scala.util.hashing.Hashing

/** Memory-safe and type-safe secret value of type `T`.
  *
  * `Secret` does the best to avoid leaking information in memory and in the code BUT an attack is possible and I don't give any certainties or
  * guarantees about security using this class, you use it at your own risk. Code is open source, you can check the implementation and take your
  * decision consciously. I'll do my best to improve the security and documentation of this class.
  *
  * <b>Obfuscation</b>
  *
  * The value is obfuscated when creating the `Secret` instance using the given `SecretStrategy` which, by default, transform the value into a xor-ed
  * `ByteBuffer` witch store bytes outside the JVM using direct memory access.
  *
  * The obfuscated value is de-obfuscated using the given `SecretStrategy` instance every time the method `use` is invoked which returns the original
  * value converting bytes back to `T` re-apply the xor.
  *
  * <b>API and Type safety</b>
  *
  * While obfuscating the value prevents or at least makes it harder to read the value from memory, Secret class API are designed to avoid leaking
  * information in other ways. Preventing developers to improperly use the secret value ( logging, etc...).
  *
  * Example
  * {{{
  *   val secretString: Secret[String]  = Secret("my_password")
  *   val database: F[Database]         = secretString.use(password => initDb(password))
  * }}}
  */
trait Secret[T] extends AutoCloseable:

  import cats.syntax.all.*

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U]

  /** Destroy the secret value by filling the obfuscated value with '\0'.
    *
    * This method is idempotent.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  def destroy()(using Location): Unit

  /** Check if the secret is destroyed
    *
    * @return
    *   `true` if the secret is destroyed, `false` otherwise
    */
  def isDestroyed: Boolean

  /** Calculate the non-deterministic hash code for this Secret.
    *
    * This hash code is NOT the hash code of the original value. It is the hash code of the obfuscated value.
    *
    * Since the obfuscated value is based on a random key, the hash code will be different every time. This function is not deterministic.
    *
    * @return
    *   the hash code of this secret. If the secret is destroyed it will return `-1`.
    */
  def hashCode(): Int

  // ------------------------------------------------------------------
  /** Avoid this method if possible. Unsafely apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Throws `SecretNoLongerValid` if the secret is destroyed
    */
  inline def unsafeUse[U](f: T => U): U =
    use[Either[SecretNoLongerValid, *], U](f).fold(throw _, identity)

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  inline def use[F[_]: MonadSecretError, U](f: T => U): F[U] =
    evalUse[F, U](f.andThen(_.pure[F]))

  /** Alias for `use` with `Either[Throwable, *]` */
  inline def useE[U](f: T => U): Either[SecretNoLongerValid, U] =
    use[Either[SecretNoLongerValid, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  inline def useAndDestroy[F[_]: MonadSecretError, U](f: T => U)(using Location): F[U] =
    evalUseAndDestroy[F, U](f.andThen(_.pure[F]))

  /** Alias for `useAndDestroy` with `Either[Throwable, *]` */
  inline def useAndDestroyE[U](f: T => U)(using Location): Either[SecretNoLongerValid, U] =
    useAndDestroy[Either[SecretNoLongerValid, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`, `evalUse`, `evalUseAndDestroy` and
    * other methods, it will raise a `NoLongerValidSecret` exception.
    */
  inline def evalUseAndDestroy[F[_]: MonadSecretError, U](f: T => F[U])(using Location): F[U] =
    evalUse(f).map { u =>
      destroy(); u
    }

  /** Alias for `destroy` */
  inline override def close(): Unit = destroy()

  /** Safely compare this secret with the provided `Secret`.
    *
    * @return
    *   `true` if the secrets are equal, `false` if they are not equal or if one of the secret is destroyed
    */
  inline def isEquals(that: Secret[T])(using Eq[T]): Boolean =
    evalUse[Try, Boolean](thisValue => that.use[Try, Boolean](_ === thisValue)).getOrElse(false)

  /** Always returns `false`, use `isEqual` instead */
  inline override def equals(obj: Any): Boolean = false

  /** @return
    *   always returns a static place holder string "** SECRET **" to avoid leaking information
    */
  inline override val toString = "** SECRET **"

object Secret extends SecretSyntax, SecretInstances:

  import cats.syntax.all.given

  final val empty: Secret[String] = plain("")

  def plain(value: String): Secret[String] =
    SecretStrategy.plainFactory {
      Secret(value)
    }

  def apply[T](value: T)(using strategy: SecretStrategy[T]): Secret[T] =
    new Secret[T] {

      private var bufferTuple: KeyValueBuffer | Null   = strategy.obfuscator(value)
      private var destructionLocation: Location | Null = _

      override def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
        if (isDestroyed)
          SecretNoLongerValid(destructionLocation).raiseError[F, U]
        else
          f(SecretStrategy[T].deObfuscator(bufferTuple))

      override def destroy()(using location: Location): Unit =
        bufferTuple.destroy()
        bufferTuple         = null
        destructionLocation = location

      override def isDestroyed: Boolean =
        bufferTuple == null

      // noinspection HashCodeUsesVar
      override def hashCode(): Int =
        if (isDestroyed) -1 else bufferTuple.obfuscatedHashCode
    }

private[secret] transparent sealed trait SecretSyntax:

  extension [T: SecretStrategy: Monoid](optSecret: Option[Secret[T]])
    def getOrEmptySecret: Secret[T] =
      optSecret.getOrElse(Secret(Monoid[T].empty))

  extension [L, T: SecretStrategy: Monoid](eSecret: Either[L, Secret[T]])
    def getOrEmptySecret: Secret[T] =
      eSecret.toOption.getOrEmptySecret

private[secret] transparent sealed trait SecretInstances:

  given [T]: Hashing[Secret[T]] =
    Hashing.fromFunction(_.hashCode())

  given [T]: Eq[Secret[T]] =
    Eq.fromUniversalEquals

  given [T]: Show[Secret[T]] =
    Show.fromToString
