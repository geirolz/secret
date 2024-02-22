package com.geirolz.secret

import cats.{Eq, Eval, MonadThrow, Monoid, Show}
import com.geirolz.secret.Secret.*
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.*

import java.nio.ByteBuffer
import scala.util.Try
import scala.util.hashing.Hashing

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
trait Secret[T] extends AutoCloseable:

  import cats.syntax.all.*

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
    */
  def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U]

  /** Destroy the secret value by filling the obfuscated value with '\0'.
    *
    * This method is idempotent.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
    */
  def destroy()(using Location): Unit

  /** @return
    *   the location where the secret was destroyed if the collection is enabled.
    */
  def destructionLocation: Option[Location]

  /** Check if the secret is destroyed
    *
    * @return
    *   `true` if the secret is destroyed, `false` otherwise
    */
  def isDestroyed: Boolean

  /** @return
    *   a hashed representation of the secret value
    */
  def hashed: String

  // ------------------------------------------------------------------
  /** map the value using the specified function.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    */
  final def map[U: SecretStrategy](f: T => U)(using Hasher): Secret[U] =
    transform(_.useE(f.andThen(Secret[U](_))))

  /** Similar to [[map]] but destroy this after the mapping */
  final def mapAndDestroy[U: SecretStrategy](f: T => U)(using Location, Hasher): Secret[U] =
    transform(_.useAndDestroyE(f.andThen(Secret[U](_))))

  /** flat map the value using the specified function.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    */
  final def flatMap[U: SecretStrategy](f: T => Secret[U])(using Hasher): Secret[U] =
    transform(_.useE(f))

  /** Similar to [[flatMap]] but destroy this after the mapping */
  final def flatMapAndDestroy[U: SecretStrategy](f: T => Secret[U])(using Location, Hasher): Secret[U] =
    transform(_.useAndDestroyE(f))

  /** Transform this secret */
  private def transform[U](t: Secret[T] => Either[SecretDestroyed, Secret[U]]): Secret[U] =
    t(this) match
      case Right(u) => u
      case Left(e)  => Secret.destroyed[U](e.destructionLocation)

  /** Directly access Secret value if not destroyed.
    *
    * The usage of this method is discouraged. Use `use*` instead.
    */
  private[secret] inline def accessValue[F[_]: MonadSecretError]: F[T] =
    use[F, T](identity)

  /** Duplicate the secret without destroying it. If this was destroyed the duplicated will also be destroyed. */
  def duplicate: Secret[T]

  /** Avoid this method if possible. Unsafely apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `SecretDestroyed` exception.
    *
    * Throws `SecretNoLongerValid` if the secret has been already destroyed
    */
  @throws[SecretDestroyed]("if the secret has been already destroyed")
  inline def unsafeUse[U](f: T => U): U =
    use[Either[SecretDestroyed, *], U](f).fold(throw _, identity)

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
  inline def useE[U](f: T => U): Either[SecretDestroyed, U] =
    use[Either[SecretDestroyed, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `SecretDestroyed` exception.
    */
  inline def useAndDestroy[F[_]: MonadSecretError, U](f: T => U)(using Location): F[U] =
    evalUseAndDestroy[F, U](f.andThen(_.pure[F]))

  /** Alias for `useAndDestroy` with `Either[Throwable, *]` */
  inline def useAndDestroyE[U](f: T => U)(using Location): Either[SecretDestroyed, U] =
    useAndDestroy[Either[SecretDestroyed, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `SecretDestroyed` exception.
    */
  inline def evalUseAndDestroy[F[_]: MonadSecretError, U](f: T => F[U])(using Location): F[U] =
    evalUse(f).map { u =>
      destroy(); u
    }

  /** Safely compare this secret with the provided `Secret`.
    *
    * @return
    *   `true` if the secrets are equal, `false` if they are not equal or if one of the secret is destroyed
    */
  inline def isEquals(that: Secret[T])(using Eq[T]): Boolean =
    evalUse[Try, Boolean](thisValue => that.use[Try, Boolean](_ === thisValue)).getOrElse(false)

  /** Safely compare the hashed value of this secret with the provided `Secret`. */
  inline def isHashedEquals(that: Secret[T]): Boolean =
    hashed == that.hashed

  /** Always returns `false` to prevents leaking information.
    *
    * Use [[isHashedEquals]] or [[isValueEquals]]
    */
  inline override def equals(obj: Any): Boolean = false

  /** Alias for `destroy` */
  inline override def close(): Unit = destroy()

  /** @return
    *   always returns a static place holder string "** SECRET **" to avoid leaking information
    */
  override final val toString: String = Secret.secretTag

object Secret extends SecretSyntax, SecretInstances:

  import cats.syntax.all.given
  export DeferredSecret.apply as defer
  export DeferredSecret.fromEnv as deferFromEnv

  private final val secretTag: String    = "** SECRET **"
  private final val destroyedTag: String = "** DESTROYED **"

  /** Create an string empty secret */
  final val empty: Secret[String] = plain("")

  /** Create a plain secret from the given value. */
  def plain(value: String)(using Hasher): Secret[String] =
    SecretStrategy.plainFactory { Secret(value) }

  /** Create a secret from the environment variable. */
  def fromEnv[F[_]: MonadThrow: SysEnv](name: String)(using SecretStrategy[String], Hasher): F[Secret[String]] =
    SysEnv[F]
      .getEnv(name)
      .flatMap(_.liftTo[F](new NoSuchElementException(s"Missing environment variable [$name]")))
      .map(Secret(_))

  /** Create a destroyed secret */
  def destroyed[T](location: Location = Location.unknown): Secret[T] = new Secret[T] {
    override def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] = SecretDestroyed(location).raiseError[F, U]
    override def destroy()(using location: Location): Unit              = ()
    override def destructionLocation: Option[Location]                  = Some(location)
    override def isDestroyed: Boolean                                   = true
    override def hashed: String                                         = destroyedTag
    override def duplicate: Secret[T]                                   = this
  }

  def noLocation[T](value: => T)(using strategy: SecretStrategy[T], hasher: Hasher): Secret[T] =
    apply(value, collectDestructionLocation = false)

  def apply[T](value: => T, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): Secret[T] =

    var bufferTuple: KeyValueBuffer | Null = strategy.obfuscator(value)
    var hashedValue: Eval[ByteBuffer] | Null = Eval.later(
      hasher.hash(
        chars   = value.toString.getBytes,
        maxSize = 12
      )
    )

    // do not use value inside the secret to avoid closure
    new Secret[T] {

      private var _destructionLocation: Location = Location.unknown

      override final def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
        if (isDestroyed)
          SecretDestroyed(_destructionLocation).raiseError[F, U]
        else
          f(SecretStrategy[T].deObfuscator(bufferTuple))

      override def duplicate: Secret[T] = map(identity)

      override final def destroy()(using location: Location): Unit =
        bufferTuple.destroy()
        bufferTuple = null
        hashedValue.map(BytesUtils.clearByteBuffer(_))
        hashedValue          = null
        _destructionLocation = if (collectDestructionLocation) location else Location.unknown

      override final def destructionLocation: Option[Location] =
        Option(_destructionLocation)

      override final def isDestroyed: Boolean =
        bufferTuple == null

      override final def hashed: String =
        if (isDestroyed)
          destroyedTag
        else
          BytesUtils.asString(hashedValue.value)
    }

private[secret] transparent sealed trait SecretSyntax:

  extension [T: SecretStrategy: Monoid](optSecret: Option[Secret[T]])(using Hasher)
    def getOrEmptySecret: Secret[T] =
      optSecret.getOrElse(Secret(Monoid[T].empty))

  extension [L, T: SecretStrategy: Monoid](eSecret: Either[L, Secret[T]])(using Hasher)
    def getOrEmptySecret: Secret[T] =
      eSecret.toOption.getOrEmptySecret

private[secret] transparent sealed trait SecretInstances:

  given [T]: Hashing[Secret[T]] =
    Hashing.fromFunction(_.hashCode())

  given [T]: Eq[Secret[T]] =
    Eq.fromUniversalEquals

  given [T]: Show[Secret[T]] =
    Show.fromToString
