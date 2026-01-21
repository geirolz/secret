package com.geirolz.secret.ziojson

import com.geirolz.secret.*
import com.geirolz.secret.internal.SecretApi
import com.geirolz.secret.strategy.SecretStrategy
import zio.json.*

given [T: JsonDecoder: SecretStrategy]: JsonDecoder[Secret[T]] =
  JsonDecoder[T].map(Secret[T](_))

given [T: JsonDecoder: SecretStrategy]: JsonDecoder[Secret.OneShot[T]] =
  JsonDecoder[T].map(Secret.oneShot[T](_))

given [S[X] <: SecretApi[X], T]: JsonEncoder[S[T]] =
  JsonEncoder.string.contramap(_.toString)

given [T: JsonDecoder: SecretStrategy]: JsonCodec[Secret[T]] =
  JsonCodec(JsonEncoder[Secret[T]], JsonDecoder[Secret[T]])

given [T: JsonDecoder: SecretStrategy]: JsonCodec[Secret.OneShot[T]] =
  JsonCodec(JsonEncoder[Secret.OneShot[T]], JsonDecoder[Secret.OneShot[T]])
