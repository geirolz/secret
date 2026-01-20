package com.geirolz.secret.circe.hash

import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.Secret
import io.circe.{Decoder, Encoder}

export com.geirolz.secret.circe.given_Decoder_Secret
export com.geirolz.secret.circe.given_Decoder_OneShot

given [S[X] <: SecretApi[X], T]: Encoder[S[T]] =
  Encoder.encodeString.contramap(_.hash)
