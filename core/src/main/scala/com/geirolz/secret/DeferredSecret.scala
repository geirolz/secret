package com.geirolz.secret

import cats.Functor
import cats.syntax.all.*
import com.geirolz.secret.internal.Location
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
  private[DeferredSecret] val acquire: F[Secret[T]]

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
  def map[U](f: T => U): DeferredSecret[F, U]

  /** FlatMap the secret value to `U` */
  def flatMap[U](f: T => DeferredSecret[F, U]): DeferredSecret[F, U]

object DeferredSecret:

  def apply[F[_]: MonadSecretError, T: SecretStrategy](acquire: F[T]): DeferredSecret[F, T] =
    DeferredSecret.fromSecret[F, T](acquire.map(Secret(_)))

  def fromSecret[F[_]: MonadSecretError, T](_acquire: F[Secret[T]]): DeferredSecret[F, T] =
    new DeferredSecret[F, T]:

      private[DeferredSecret] val acquire = _acquire

      def use[U](f: T => U): F[U] =
        acquire.flatMap(_.useAndDestroy(f))

      def evalUse[U](f: T => F[U]): F[U] =
        acquire.flatMap(_.evalUseAndDestroy(f))

      def map[U](f: T => U): DeferredSecret[F, U] =
        DeferredSecret.fromSecret(acquire.map(_.map(f)))

      def flatMap[U](f: T => DeferredSecret[F, U]): DeferredSecret[F, U] =
        DeferredSecret.fromSecret(evalUse(f(_).acquire))

private[secret] sealed trait DeferredSecretInstances:

  given [F[_]]: Functor[DeferredSecret[F, *]] with
    def map[A, B](fa: DeferredSecret[F, A])(f: A => B): DeferredSecret[F, B] = fa.map(f)
