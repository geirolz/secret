package com.geirolz.secret.circe.hashed

import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.{OneShotSecret, Secret}
import io.circe.{Decoder, Encoder}

export com.geirolz.secret.circe.given_Decoder_Secret
export com.geirolz.secret.circe.given_Decoder_OneShotSecret

given [S[X] <: SecretApi[X], T]: Encoder[S[T]] =
  Encoder.encodeString.contramap(_.hashed)
