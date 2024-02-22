package com.geirolz.secret

import cats.effect.{Async, IO, Resource}

extension [T](secret: Secret[T])
  
  inline def resource[F[_]: Async]: Resource[F, T] =
    IO.fromFuture(???)
    
    Resource
      .fromAutoCloseable(secret.duplicate[F])
      .evalMap(_.accessValue[F])
