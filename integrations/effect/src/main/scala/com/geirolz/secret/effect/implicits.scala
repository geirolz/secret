package com.geirolz.secret.effect

import cats.effect.Async
import cats.effect.{IO, Resource, ResourceIO}
import com.geirolz.secret.Secret

extension [T](secret: Secret[T])

  def resource[F[_]: Async]: Resource[F, Secret[T]] =
    Resource.make(Async[F].pure(secret))(s => Async[F].delay(s.destroy()))

  def resourceIO: ResourceIO[Secret[T]] = resource[IO]
