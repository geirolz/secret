package com.geirolz.secret.internal

import cats.syntax.all.*
import cats.{Eq, MonadThrow, Monoid, Show}
import com.geirolz.secret.*
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.transform.Hasher
import com.geirolz.secret.util.{Location, SysEnv}

import java.nio.charset.{Charset, StandardCharsets}
import scala.util.hashing.Hashing

private[secret] transparent trait SecretApi[T](
  protected val vault: Vault[T]
) extends AutoCloseable:

  import cats.syntax.all.*

  type Self[X] <: SecretApi[X]

  private[secret] val companion: SecretCompanionApi[[X] =>> Self[X]]

  /** Destroy the secret value by filling the obfuscated value with '\0'.
    *
    * This method is idempotent.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `NoLongerValidSecret` exception.
    */
  final def destroy()(using Location): Unit =
    vault.destroy()

  /** @return
    *   the location where the secret was destroyed if the collection is enabled.
    */
  final def destructionLocation: Option[Location] =
    vault.destructionLocation

  /** Check if the secret is destroyed
    *
    * @return
    *   `true` if the secret is destroyed, `false` otherwise
    */
  final def isDestroyed: Boolean =
    vault.isDestroyed

  // ---------------- USAGE METHODS ----------------

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `SecretDestroyed` exception.
    */
  inline final def useAndDestroy[F[_]: MonadSecretError, U](f: T => U)(using Location): F[U] =
    evalUseAndDestroy[F, U](f.andThen(_.pure[F]))

  /** Alias for `useAndDestroy` with `Either[Throwable, *]` */
  inline final def euseAndDestroy[U](f: T => U)(using Location): Either[SecretDestroyed, U] =
    useAndDestroy[Either[SecretDestroyed, *], U](f)

  /** Apply `f` with the de-obfuscated value and then destroy the secret value by invoking `destroy` method.
    *
    * Once the secret is destroyed it can't be used anymore. If you try to use it using `use`, `useAndDestroy`,
    * `evalUse`, `evalUseAndDestroy` and other methods, it will raise a `SecretDestroyed` exception.
    */
  inline final def evalUseAndDestroy[F[_]: MonadSecretError, U](f: T => F[U])(using Location): F[U] =
    vault.evalUse(f).map { u =>
      destroy(); u
    }

  // ---------------- TRANSFORMATIONS METHODS ----------------
  /** Creates a new secret mapping the value using the specified function and then destroy this.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    */
  def mapAndDestroy[U: SecretStrategy](f: T => U)(using Location): Self[U] =
    transform(
      _.euseAndDestroy(
        f.andThen(newFromThis(_))
      )
    )

  /** Flat map the value using the specified function and then destroy this and returns the new Secret returned by the
    * specified function.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    */
  final def flatMapAndDestroy[U: SecretStrategy](f: T => Self[U])(using Location): Self[U] =
    transform(_.useAndDestroy(f))

  /** Creates a new secret that contains the hashed value of the secret and destroy the current one.
    *
    * The secret value is hashed using the specified `hasher` and `charset`.
    *
    * By default the charset is `StandardCharsets.UTF_8` and the hasher is the one used by the secret.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    *
    * @param hasher
    *   the hasher to use
    * @param charset
    *   the charset to use
    */
  final def asHashedAndDestroy(
    hasher: Hasher   = vault._hasher,
    charset: Charset = StandardCharsets.UTF_8
  ): Self[String] =
    flatMapAndDestroy[String] { v =>
      newFromThis(
        hasher.hashAsString(v.toString.getBytes(charset))
      )
    }

  // ---------------- DATA ----------------
  /** @return
    *   a hashed representation of the secret value
    */
  final def hash: String = vault.hash

  /** Safely compare the hash value of this secret with the provided `Secret`. */
  inline def isHashEquals(that: Secret[T]): Boolean =
    hash == that.hash

  /** Always returns `false` to prevents leaking information.
    *
    * Use [[isHashEquals]] or [[isValueEquals]]
    */
  inline final override def equals(obj: Any): Boolean = false

  /** Alias for `destroy` */
  inline final override def close(): Unit = destroy()

  /** @return
    *   always returns a static place holder string "** SECRET **" to avoid leaking information
    */
  override final val toString: String = secretTag

  // private methods

  /** Directly access Secret value if not destroyed.
    *
    * The usage of this method is discouraged. Use `use*` instead.
    */
  private[secret] inline def accessValue[F[_]: MonadSecretError]: F[T] =
    vault.evalUse[F, T](_.pure[F])

  /** Transform this secret */
  private[secret] def transform[U](t: Self[T] => Either[SecretDestroyed, Self[U]]): Self[U] =
    t(this.asInstanceOf[Self[T]]) match
      case Right(u) => u
      case Left(e)  => companion.destroyed[U](e.destructionLocation)

  /** Create a new secret from this Secret settings */
  private[secret] def newFromThis[U: SecretStrategy](value: U): Self[U] =
    given Hasher = vault._hasher
    companion[U](
      value                  = value,
      recDestructionLocation = vault._recDestructionLocation
    )

private[secret] transparent trait SecretCompanionApi[SecretTpe[X] <: SecretApi[X]]
    extends SecretApiSyntax[SecretTpe],
      SecretApiInstances[SecretTpe]:

  /** Create a destroyed secret */
  def destroyed[T](location: Location = Location.unknown): SecretTpe[T]

  /** Create a secret from the given value. */
  def apply[T](value: => T, recDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): SecretTpe[T]

  /** Create an string empty secret */
  final val empty: SecretTpe[String] = plain("")

  /** Create a plain secret from the given value. */
  def plain(value: String)(using Hasher): SecretTpe[String] =
    SecretStrategy.plainFactory { apply(value) }

  /** Create a secret from the environment variable. */
  def fromEnv[F[_]: {MonadThrow, SysEnv}](name: String)(using SecretStrategy[String], Hasher): F[SecretTpe[String]] =
    SysEnv[F]
      .getEnv(name)
      .flatMap(_.liftTo[F](new NoSuchElementException(s"Missing environment variable [$name]")))
      .map(apply(_))

/** Syntax for the SecretPlatform */
private[secret] transparent sealed trait SecretApiSyntax[SecretTpe[X] <: SecretApi[X]]:
  this: SecretCompanionApi[SecretTpe] =>

  extension [T: {SecretStrategy, Monoid}](optSecret: Option[SecretTpe[T]])(using Hasher)
    def getOrEmptySecret: SecretTpe[T] =
      optSecret.getOrElse(this.apply(Monoid[T].empty))

  extension [L, T: {SecretStrategy, Monoid}](eSecret: Either[L, SecretTpe[T]])(using Hasher)
    def getOrEmptySecret: SecretTpe[T] =
      eSecret.toOption.getOrEmptySecret

/** Instances for the SecretPlatform */
private[secret] transparent sealed trait SecretApiInstances[SecretTpe[X] <: SecretApi[X]]:

  given [T] => Hashing[SecretTpe[T]] =
    Hashing.fromFunction(_.hashCode())

  given [T] => Eq[SecretTpe[T]] =
    Eq.fromUniversalEquals

  given [T] => Show[SecretTpe[T]] =
    Show.fromToString
