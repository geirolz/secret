package com.geirolz.secret.effect

import cats.effect.{Async, Resource}
import com.geirolz.secret.Secret

extension [T](secret: Secret[T])
  def resource[F[_]: Async]: Resource[F, Secret[T]] =
    Resource.make(Async[F].pure(secret))(s => Async[F].delay(s.destroy()))
