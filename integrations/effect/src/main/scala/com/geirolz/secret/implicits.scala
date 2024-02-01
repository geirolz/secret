package com.geirolz.secret

import cats.effect.{Async, Resource}

extension [T](secret: Secret[T])
  inline def resource[F[_]: Async]: Resource[F, T] =
    Resource
      .make(secret.duplicate[F])(s => Async[F].delay(s.destroy()))
      .evalMap(_.accessValue[F])
