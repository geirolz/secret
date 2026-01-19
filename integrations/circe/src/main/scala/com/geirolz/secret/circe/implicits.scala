package com.geirolz.secret.circe

import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.Secret
import com.geirolz.secret.strategy.SecretStrategy
import io.circe.{Decoder, Encoder}

given [T: {Decoder, SecretStrategy}] => Decoder[Secret[T]] =
  Decoder[T].map(Secret[T](_))

given [T: {Decoder, SecretStrategy}] => Decoder[Secret.OneShot[T]] =
  Decoder[T].map(Secret.oneShot[T](_))

given [S[X] <: SecretApi[X], T] => Encoder[S[T]] =
  Encoder.encodeString.contramap(_.toString)
