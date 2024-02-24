package com.geirolz.secret

import cats.{Eval, Functor, MonadThrow}
import cats.syntax.all.*
import com.geirolz.secret.util.{Hasher, Location}
import com.geirolz.secret.strategy.SecretStrategy

/** Specialized version of `Secret` that allows to defer the acquisition of the secret value. This is useful when you
  * want to acquire the secret value only when it's needed and not before ( for instance, an HTTP call to a secret
  * manager).
  *
  * Use this type only when you need to defer the acquisition of the secret value. If you have the secret value at the
  * moment of the creation of the `DeferredSecret` use the `Secret` type instead.
  *
  * @tparam F
  *   effect type
  * @tparam T
  *   secret type
  */
sealed trait DeferredSecret[F[_], T]:

  /** Acquire the secret value. This method is called every time you use the DeferredSecret. */
  private[secret] def acquire: F[Secret[T]]

  /** This method acquire the Secret value every time and once used destroy the secret. It doesn't has the suffix
    * "AndDestroy" because it's the default behavior of the DeferredSecret which could be called any number of times
    * since it re-create every time.
    */
  def use[U](f: T => U): F[U]

  /** This method acquire the Secret value every time and once used destroy the secret. It doesn't has the suffix
    * "AndDestroy" because it's the default behavior of the DeferredSecret which could be called any number of times
    * since it re-create every time.
    */
  def evalUse[U](f: T => F[U]): F[U]

  /** Map the secret value to `U` */
  def map[U: SecretStrategy](f: T => U)(using Hasher): DeferredSecret[F, U]

  /** FlatMap the secret value to `U` */
  def flatMap[U: SecretStrategy](f: T => DeferredSecret[F, U])(using Hasher): DeferredSecret[F, U]

  /** Handle the error of the acquisition of the secret value */
  def handleError(f: Throwable => Secret[T]): DeferredSecret[F, T]

  /** Handle the error of the acquisition of the secret value */
  def handleErrorWith(f: Throwable => F[Secret[T]]): DeferredSecret[F, T]

object DeferredSecret:

  /** Create a DeferredSecret from a `F[T]`.
    *
    * The function is called every time the DeferredSecret is used.
    */
  def apply[F[_]: MonadThrow, T: SecretStrategy](acquire: => F[T])(using Hasher): DeferredSecret[F, T] =
    DeferredSecret.fromSecret[F, T](acquire.map(Secret(_)))

  /** Create a pure and constant DeferredSecret */
  def pure[F[_]: MonadThrow, T: SecretStrategy](t: T)(using Hasher): DeferredSecret[F, T] =
    DeferredSecret(t.pure[F])

  /** Create a failed DeferredSecret which always fails with the specified error */
  def failed[F[_]: MonadThrow, T](e: Throwable): DeferredSecret[F, T] =
    DeferredSecret.fromSecret(MonadThrow[F].raiseError(e))

  /** Create a DeferredSecret that reads the specified environment variable every time it is used. */
  def fromEnv[F[_]: MonadThrow](name: String)(using SecretStrategy[String], Hasher): DeferredSecret[F, String] =
    DeferredSecret.fromSecret(Secret.fromEnv[F](name))

  /** Create a DeferredSecret from a Secret.
    *
    * The acquire function is called every time you use the DeferredSecret.
    */
  def fromSecret[F[_]: MonadThrow, T](_acquire: => F[Secret[T]]): DeferredSecret[F, T] =
    new DeferredSecret[F, T]:

      private[secret] def acquire = _acquire

      override def use[U](f: T => U): F[U] =
        acquire.flatMap(_.useAndDestroy(f))

      override def evalUse[U](f: T => F[U]): F[U] =
        acquire.flatMap(_.evalUseAndDestroy(f))

      override def map[U: SecretStrategy](f: T => U)(using Hasher): DeferredSecret[F, U] =
        DeferredSecret.fromSecret(acquire.map(_.map(f)))

      override def flatMap[U: SecretStrategy](f: T => DeferredSecret[F, U])(using Hasher): DeferredSecret[F, U] =
        DeferredSecret.fromSecret(evalUse(f(_).acquire))

      override def handleError(f: Throwable => Secret[T]): DeferredSecret[F, T] =
        handleErrorWith(f.andThen(_.pure[F]))

      override def handleErrorWith(f: Throwable => F[Secret[T]]): DeferredSecret[F, T] =
        DeferredSecret.fromSecret(acquire.handleErrorWith(f))
