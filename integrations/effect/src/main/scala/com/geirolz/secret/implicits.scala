package com.geirolz.secret

import cats.effect.{Async, Resource}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.Hasher

// one shot secret
extension [T](secret: Secret.OneShot[T])
  inline def resourceDestroy[F[_]](using F: Async[F]): Resource[F, T] =
    Resource
      .fromAutoCloseable(F.delay(secret))
      .evalMap(_.accessValue[F])

extension (obj: OneShotSecret.type)
  def resource[F[_]: Async, T: SecretStrategy](secret: T)(using Hasher): Resource[F, T] =
    OneShotSecret(secret).resourceDestroy[F]

// secret
extension [T](secret: Secret[T])
  inline def resource[F[_]](using F: Async[F]): Resource[F, T] =
    Resource
      .fromAutoCloseable(F.defer(F.delay(secret.duplicate)))
      .evalMap(_.accessValue[F])

  inline def resourceDestroy[F[_]](using F: Async[F]): Resource[F, T] =
    Resource
      .fromAutoCloseable(F.defer(F.delay(secret)))
      .evalMap(_.accessValue[F])

extension (obj: Secret.type)
  def resource[F[_]: Async, T: SecretStrategy](secret: T)(using Hasher): Resource[F, T] =
    Secret(secret).resourceDestroy[F]

// deferred secret
extension [F[_], T](secret: Secret.Deferred[F, T])(using F: Async[F])
  inline def resource: Resource[F, T] =
    Resource
      .make(secret.acquire)(s => F.delay(s.destroy()))
      .evalMap(_.accessValue[F])

extension (obj: DeferredSecret.type)
  def resource[F[_]: Async, T: SecretStrategy](acquire: => F[T])(using Hasher): Resource[F, T] =
    DeferredSecret(acquire).resource
