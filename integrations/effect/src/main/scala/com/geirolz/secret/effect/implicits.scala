package com.geirolz.secret.effect

import cats.effect.{Async, Resource}
import com.geirolz.secret.Secret

extension [T](secret: Secret[T])
  inline def resource[F[_]: Async]: Resource[F, T] =
    Resource
      .make(secret.duplicate[F])(s => Async[F].delay(s.destroy()))
      .evalMap(_.accessValue[F])
