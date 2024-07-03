package com.geirolz.secret.catsxml.hashed

import cats.xml.codec.Encoder
import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.{OneShotSecret, Secret}

export com.geirolz.secret.catsxml.given_Decoder_Secret
export com.geirolz.secret.catsxml.given_Decoder_OneShotSecret

given [S[X] <: SecretApi[X], T]: Encoder[S[T]] =
  Encoder.encodeString.contramap(_.hashed)
