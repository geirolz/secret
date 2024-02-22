package com.geirolz.secret

import cats.{Eq, Functor, MonadThrow, Monoid, Show}
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

  /** Check if the secret is destroyed
    *
    * @return
    *   `true` if the secret is destroyed, `false` otherwise
    */
  def isDestroyed: Boolean

  /** @return
    *   the hash code of the secret hashed value
    */
  def hashCode(): Int

  /** @return
    *   a hashed representation of the secret value
    */
  def hashed: String

  /** Create another Secret with the same value if this has not been destroyed.
    * @return
    */
  private[secret] def duplicate[F[_]: MonadSecretError]: F[Secret[T]]
  private[secret] inline def duplicateE: Either[SecretDestroyed, Secret[T]] =
    duplicate[Either[SecretDestroyed, *]]

  // ------------------------------------------------------------------

  /** Duplicate the secret and map the value using the specified function.
    *
    * If the secret were destroyed it will raise a `NoLongerValidSecret` when you try to use the new secret.
    */
  final def map[U](f: T => U): Secret[U] =
    SecretMap(this.duplicateE, f)

  /** Duplicate the secret and flat map the value using the specified function.
    *
    * If the secret were destroyed it will raise a `NoLongerValidSecret` when you try to use the new secret.
    */
  final def flatMap[U](f: T => Secret[U]): Secret[U] =
    SecretFlatMap(this.duplicateE, f)

  /** Directly access Secret value if not destroyed.
    *
    * The usage of this method is discouraged. Use `use*` instead.
    */
  private[secret] inline def accessValue[F[_]: MonadSecretError]: F[T] =
    use[F, T](identity)

  /** Avoid this method if possible. Unsafely apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Throws `SecretNoLongerValid` if the secret has been already destroyed
    */
  @throws[SecretDestroyed]("if the secret has been already destroyed")
  inline def unsafeUse[U](f: T => U): U =
    use[Either[SecretDestroyed, *], U](f).fold(throw _, identity)

  /** Apply `f` with the de-obfuscated value WITHOUT destroying it.
    *
    * If the secret is destroyed it will raise a `NoLongerValidSecret` exception.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
    */
  inline def use[F[_]: MonadSecretError, U](f: T => U): F[U] =
    evalUse[F, U](f.andThen(_.pure[F]))

  /** Alias for `use` with `Either[Throwable, *]` */
  inline def useE[U](f: T => U): Either[SecretDestroyed, U] =
    use[Either[SecretDestroyed, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
    */
  inline def useAndDestroy[F[_]: MonadSecretError, U](f: T => U)(using Location): F[U] =
    evalUseAndDestroy[F, U](f.andThen(_.pure[F]))

  /** Alias for `useAndDestroy` with `Either[Throwable, *]` */
  inline def useAndDestroyE[U](f: T => U)(using Location): Either[SecretDestroyed, U] =
    useAndDestroy[Either[SecretDestroyed, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
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

  /** Check if `that` a Secret and it's equals to `this` comparing the hashed value.
    *
    * Return false if one of these is destroyed
    */
  inline override def equals(obj: Any): Boolean =
    obj match
      case that: Secret[?] =>
        given Eq[T] = Eq.fromUniversalEquals
        Try(this.isEquals(that.asInstanceOf[Secret[T]])).getOrElse(false)
      case _ => false

  /** Alias for `destroy` */
  inline override def close(): Unit = destroy()

  /** @return
    *   always returns a static place holder string "** SECRET **" to avoid leaking information
    */
  inline override val toString = "** SECRET **"

object Secret extends SecretSyntax, SecretInstances:

  import cats.syntax.all.given
  export DeferredSecret.apply as defer
  export DeferredSecret.fromEnv as deferFromEnv

  private val destroyedTag = "** DESTROYED **"

  final val empty: Secret[String] = plain("")

  def plain(value: String)(using Hasher): Secret[String] =
    SecretStrategy.plainFactory { Secret(value) }

  def fromEnv[F[_]: MonadThrow: SysEnv](name: String)(using SecretStrategy[String], Hasher): F[Secret[String]] =
    SysEnv[F]
      .getEnv(name)
      .flatMap(_.liftTo[F](new NoSuchElementException(s"Missing environment variable [$name]")))
      .map(Secret(_))

  def apply[T](value: => T)(using strategy: SecretStrategy[T], hasher: Hasher): Secret[T] =
    var bufferTuple: KeyValueBuffer | Null = strategy.obfuscator(value)
    var hashedValue: ByteBuffer | Null     = hasher.hash(value.toString.toCharArray, 12)
    var hashcode: Int | Null               = hashedValue.hashCode()

    // do not use value inside the secret to avoid closure
    new Secret[T] {

      private var destructionLocation: Location | Null = _

      override final def evalUse[F[_]: MonadSecretError, U](f: T => F[U]): F[U] =
        if (isDestroyed)
          SecretDestroyed(destructionLocation).raiseError[F, U]
        else
          f(SecretStrategy[T].deObfuscator(bufferTuple))

      override private[secret] def duplicate[F[_]: MonadSecretError]: F[Secret[T]] =
        use[F, Secret[T]](value => Secret(value))

      override final def destroy()(using location: Location): Unit =
        bufferTuple.destroy()
        bufferTuple         = null
        hashcode            = -1
        hashedValue         = BytesUtils.clearByteBuffer(hashedValue)
        destructionLocation = location

      override final def isDestroyed: Boolean =
        bufferTuple == null

      override final def hashCode(): Int =
        if (isDestroyed)
          -1
        else
          hashcode.asInstanceOf[Int]

      override final def hashed: String =
        if (isDestroyed)
          destroyedTag
        else
          BytesUtils.asString(hashedValue)
    }

  private sealed transparent trait SecretDelegated[T, U](
    protected val duplicateFun: Secret[T] => Secret[U]
  ) extends Secret[U]:
    protected val underlying: Either[SecretDestroyed, Secret[T]]

    override inline def evalUse[F[_]: MonadSecretError, V](f: U => F[V]): F[V] =
      underlying match
        case Right(s) => delegatedUse[F, V](s, f)
        case Left(e)  => e.raiseError[F, V]

    override private[secret] def duplicate[F[_]: MonadSecretError]: F[Secret[U]] =
      summon[MonadSecretError[F]]
        .fromEither(underlying)
        .map(duplicateFun)

    protected def delegatedUse[F[_]: MonadSecretError, V](s: Secret[T], f: U => F[V]): F[V]
    override final def destroy()(using location: Location): Unit =
      underlying.foreach(_.destroy())
    override final def isDestroyed: Boolean =
      underlying.forall(_.isDestroyed)
    override final def hashCode(): Int =
      underlying.map(_.hashCode()).getOrElse(-1)
    override final def hashed: String =
      underlying.map(_.hashed).getOrElse(destroyedTag)

  private class SecretMap[T, U](
    protected val underlying: Either[SecretDestroyed, Secret[T]],
    protected val mapF: T => U
  ) extends SecretDelegated[T, U](duplicateFun = _.map(mapF)):
    override protected def delegatedUse[F[_]: MonadSecretError, V](s: Secret[T], f: U => F[V]): F[V] =
      s.evalUse(f.compose(mapF))

  private class SecretFlatMap[T, U](
    protected val underlying: Either[SecretDestroyed, Secret[T]],
    protected val mapF: T => Secret[U]
  ) extends SecretDelegated[T, U](duplicateFun = _.flatMap(mapF)):
    override protected def delegatedUse[F[_]: MonadSecretError, V](s: Secret[T], f: U => F[V]): F[V] =
      s.evalUse(v => mapF(v).evalUse(f))

private[secret] transparent sealed trait SecretSyntax:

  extension [T: SecretStrategy: Monoid](optSecret: Option[Secret[T]])(using Hasher)
    def getOrEmptySecret: Secret[T] =
      optSecret.getOrElse(Secret(Monoid[T].empty))

  extension [L, T: SecretStrategy: Monoid](eSecret: Either[L, Secret[T]])(using Hasher)
    def getOrEmptySecret: Secret[T] =
      eSecret.toOption.getOrEmptySecret

private[secret] transparent sealed trait SecretInstances:

  given Functor[Secret] = new Functor[Secret]:
    override def map[A, B](fa: Secret[A])(f: A => B): Secret[B] =
      fa.map(f)

  given [T]: Hashing[Secret[T]] =
    Hashing.fromFunction(_.hashCode())

  given [T]: Eq[Secret[T]] =
    Eq.fromUniversalEquals

  given [T]: Show[Secret[T]] =
    Show.fromToString
