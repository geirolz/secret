package com.geirolz.secret.catsxml.hash

import cats.xml.codec.Encoder
import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.Secret

export com.geirolz.secret.catsxml.given_Decoder_Secret
export com.geirolz.secret.catsxml.given_Decoder_OneShot

given [S[X] <: SecretApi[X], T]: Encoder[S[T]] =
  Encoder.encodeString.contramap(_.hash)
