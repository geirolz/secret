package com.geirolz.secret

import cats.effect.{Async, Resource}
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.util.Hasher

extension (secret: Secret.type)
  inline def resource[F[_]: Async, T: SecretStrategy](value: => T)(using Hasher): Resource[F, T] =
    Secret(value).resource[F]

extension [T](secret: Secret[T])
  inline def resource[F[_]: Async]: Resource[F, T] =
    Resource
      .fromAutoCloseable(Async[F].defer(Async[F].delay(secret.duplicate)))
      .evalMap(_.accessValue[F])
