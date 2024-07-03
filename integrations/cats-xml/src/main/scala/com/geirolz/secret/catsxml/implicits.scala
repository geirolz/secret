package com.geirolz.secret.catsxml

import cats.xml.codec.{Decoder, Encoder}
import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.{OneShotSecret, Secret}
import com.geirolz.secret.strategy.SecretStrategy

given [T: Decoder: SecretStrategy]: Decoder[Secret[T]] =
  Decoder[T].map(Secret[T](_))

given [T: Decoder: SecretStrategy]: Decoder[OneShotSecret[T]] =
  Decoder[T].map(OneShotSecret[T](_))

given [S[X] <: SecretApi[X], T]: Encoder[S[T]] =
  Encoder.encodeString.contramap(_.toString)
