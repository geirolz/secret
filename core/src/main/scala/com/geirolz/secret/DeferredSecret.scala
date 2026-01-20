package com.geirolz.secret

import cats.MonadThrow
import cats.syntax.all.*
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.transform.Hasher
import com.geirolz.secret.util.Location
import com.geirolz.secret.util.SysEnv

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
  def use[U](f: T => U)(using Location): F[U]

  /** Acquire the secret value and transform it.
    *
    * The secret is be destroyed after the use
    */
  def useRaw[U](f: Secret[T] => U): F[U]

  /** This method acquire the Secret value every time and once used destroy the secret. It doesn't has the suffix
    * "AndDestroy" because it's the default behavior of the DeferredSecret which could be called any number of times
    * since it re-create every time.
    */
  def evalUse[U](f: T => F[U])(using Location): F[U]

  /** Acquire the secret value and transform it in `F`.
    *
    * The secret is be destroyed after the use
    */
  def evalUseRaw[U](f: Secret[T] => F[U]): F[U]

  /** Map the secret value to `U` */
  def map[U: SecretStrategy](f: T => U)(using Hasher): Secret.Deferred[F, U]

  /** FlatMap the secret value to `U` */
  def flatMap[U: SecretStrategy](f: T => Secret.Deferred[F, U])(using Hasher, Location): Secret.Deferred[F, U]

  /** Handle the error of the acquisition of the secret value */
  def handleError(f: Throwable => Secret[T]): Secret.Deferred[F, T]

  /** Handle the error of the acquisition of the secret value */
  def handleErrorWith(f: Throwable => F[Secret[T]]): Secret.Deferred[F, T]

object DeferredSecret:

  /** Create a DeferredSecret from a `F[T]`.
    *
    * The function is called every time the DeferredSecret is used.
    */
  def apply[F[_]: MonadThrow, T: SecretStrategy](acquire: => F[T])(using Hasher): Secret.Deferred[F, T] =
    DeferredSecret.fromSecret[F, T](acquire.map(Secret(_)))

  /** Create a pure and constant DeferredSecret */
  def pure[F[_]: MonadThrow, T: SecretStrategy](t: T)(using Hasher): Secret.Deferred[F, T] =
    DeferredSecret(t.pure[F])

  /** Create a failed DeferredSecret which always fails with the specified error */
  def failed[F[_]: MonadThrow, T](e: Throwable): Secret.Deferred[F, T] =
    DeferredSecret.fromSecret(MonadThrow[F].raiseError(e))

  /** Create a DeferredSecret that reads the specified environment variable every time it is used. */
  def fromEnv[F[_]: MonadThrow: SysEnv](
    name: String
  )(using SecretStrategy[String], Hasher): Secret.Deferred[F, String] =
    DeferredSecret.fromSecret(Secret.fromEnv[F](name))

  /** Create a DeferredSecret from a Secret.
    *
    * The acquire function is called every time you use the DeferredSecret.
    */
  def fromSecret[F[_]: MonadThrow, T](_acquire: => F[Secret[T]]): Secret.Deferred[F, T] =
    new Secret.Deferred[F, T]:

      private[secret] def acquire = _acquire

      override def useRaw[U](f: Secret[T] => U): F[U] =
        acquire.map(f)

      override def use[U](f: T => U)(using Location): F[U] =
        acquire.flatMap(_.useAndDestroy[F, U](f))

      override def evalUse[U](f: T => F[U])(using Location): F[U] =
        acquire.flatMap(_.evalUseAndDestroy[F, U](f))

      override def evalUseRaw[U](f: Secret[T] => F[U]): F[U] =
        acquire.flatMap(f)

      override def map[U: SecretStrategy](f: T => U)(using Hasher): Secret.Deferred[F, U] =
        DeferredSecret.fromSecret(acquire.map(_.map(f)))

      override def flatMap[U: SecretStrategy](
        f: T => Secret.Deferred[F, U]
      )(using Hasher, Location): Secret.Deferred[F, U] =
        DeferredSecret.fromSecret(evalUse(f(_).acquire))

      override def handleError(f: Throwable => Secret[T]): Secret.Deferred[F, T] =
        handleErrorWith(f.andThen(_.pure[F]))

      override def handleErrorWith(f: Throwable => F[Secret[T]]): Secret.Deferred[F, T] =
        DeferredSecret.fromSecret(acquire.handleErrorWith(f))
