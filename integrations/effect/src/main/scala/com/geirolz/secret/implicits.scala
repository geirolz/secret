package com.geirolz.secret

import cats.effect.{Async, Resource}

// secret
extension [T](secret: Secret[T])
  inline def resource[F[_]: Async]: Resource[F, T] =
    Resource
      .fromAutoCloseable(Async[F].defer(Async[F].delay(secret.duplicate)))
      .evalMap(_.accessValue[F])

// deferred secret
extension [F[_]: Async, T](secret: DeferredSecret[F, T])
  inline def resource: Resource[F, T] =
    Resource
      .make(secret.acquire)(s => Async[F].delay(s.destroy()))
      .evalMap(_.accessValue[F])
