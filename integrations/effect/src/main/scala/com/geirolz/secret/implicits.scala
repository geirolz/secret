package com.geirolz.secret

import cats.effect.{Async, Resource}

// one shot secret
extension [T](secret: OneShotSecret[T])
  inline def resource[F[_]](using F: Async[F]): Resource[F, T] =
    Resource
      .fromAutoCloseable(F.delay(secret))
      .evalMap(_.accessValue[F])

// secret
extension [T](secret: Secret[T])
  inline def resource[F[_]](using F: Async[F]): Resource[F, T] =
    Resource
      .fromAutoCloseable(F.defer(F.delay(secret.duplicate)))
      .evalMap(_.accessValue[F])

// deferred secret
extension [F[_], T](secret: DeferredSecret[F, T])(using F: Async[F])
  inline def resource: Resource[F, T] =
    Resource
      .make(secret.acquire)(s => F.delay(s.destroy()))
      .evalMap(_.accessValue[F])
