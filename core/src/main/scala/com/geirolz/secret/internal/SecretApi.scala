package com.geirolz.secret.internal

import cats.syntax.all.*
import cats.{Eq, MonadThrow, Monoid, Show}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.{Hasher, Location, SysEnv}
import com.geirolz.secret.*

import scala.util.hashing.Hashing

private[secret] transparent trait SecretApi[T](protected val vault: Vault[T]) extends AutoCloseable:

  import cats.syntax.all.*

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

  /** map the value using the specified function and then destroy this.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    */
  final def mapAndDestroy[U: SecretStrategy](f: T => U)(using Location, Hasher): Secret[U] =
    transform(_.useAndDestroy(f.andThen(Secret[U](_))))

  /** flat map the value using the specified function and then destroy this.
    *
    * If the secret were destroyed it will raise a `SecretDestroyed` when you try to use the new secret.
    */
  final def flatMapAndDestroy[U: SecretStrategy](f: T => Secret[U])(using Location, Hasher): Secret[U] =
    transform(_.useAndDestroy(f))

  // ---------------- DATA ----------------

  /** @return
    *   a hashed representation of the secret value
    */
  final def hashed: String = vault.hashed

  /** Safely compare the hashed value of this secret with the provided `Secret`. */
  inline def isHashedEquals(that: Secret[T]): Boolean =
    hashed == that.hashed

  /** Always returns `false` to prevents leaking information.
    *
    * Use [[isHashedEquals]] or [[isValueEquals]]
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
  private[secret] def transform[U](t: this.type => Either[SecretDestroyed, Secret[U]]): Secret[U] =
    t(this) match
      case Right(u) => u
      case Left(e)  => Secret.destroyed[U](e.destructionLocation)

private[secret] transparent trait SecretCompanionApi[SecretTpe[X] <: SecretApi[X]]
    extends SecretApiSyntax[SecretTpe],
      SecretApiInstances[SecretTpe]:

  /** Create a destroyed secret */
  def destroyed[T](location: Location = Location.unknown): SecretTpe[T]

  /** Create a secret from the given value. */
  def apply[T](value: => T, collectDestructionLocation: Boolean = true)(using
    strategy: SecretStrategy[T],
    hasher: Hasher
  ): SecretTpe[T]

  /** Create an string empty secret */
  final val empty: SecretTpe[String] = plain("")

  /** Create a plain secret from the given value. */
  def plain(value: String)(using Hasher): SecretTpe[String] =
    SecretStrategy.plainFactory { apply(value) }

  /** Create a secret from the environment variable. */
  def fromEnv[F[_]: MonadThrow: SysEnv](name: String)(using SecretStrategy[String], Hasher): F[SecretTpe[String]] =
    SysEnv[F]
      .getEnv(name)
      .flatMap(_.liftTo[F](new NoSuchElementException(s"Missing environment variable [$name]")))
      .map(apply(_))

  /** Create a new secret with the given value without collecting the destruction location */
  def noLocation[T: SecretStrategy](value: => T)(using Hasher): SecretTpe[T] =
    apply(value, collectDestructionLocation = false)

/** Syntax for the SecretPlatform */
private[secret] transparent sealed trait SecretApiSyntax[SecretTpe[X] <: SecretApi[X]]:
  this: SecretCompanionApi[SecretTpe] =>

  extension [T: SecretStrategy: Monoid](optSecret: Option[SecretTpe[T]])(using Hasher)
    def getOrEmptySecret: SecretTpe[T] =
      optSecret.getOrElse(this.apply(Monoid[T].empty))

  extension [L, T: SecretStrategy: Monoid](eSecret: Either[L, SecretTpe[T]])(using Hasher)
    def getOrEmptySecret: SecretTpe[T] =
      eSecret.toOption.getOrEmptySecret

/** Instances for the SecretPlatform */
private[secret] transparent sealed trait SecretApiInstances[SecretTpe[X] <: SecretApi[X]]:

  given [T]: Hashing[SecretTpe[T]] =
    Hashing.fromFunction(_.hashCode())

  given [T]: Eq[SecretTpe[T]] =
    Eq.fromUniversalEquals

  given [T]: Show[SecretTpe[T]] =
    Show.fromToString
