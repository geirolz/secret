package com.geirolz.secret.ciris

import cats.effect.IO
import ciris.ConfigValue
import com.geirolz.secret.Secret
import weaver.*

object CirisSecretSupportSuite extends SimpleIOSuite:

  test("Read OneShotSecret string with ciris") {

    val result: IO[Secret.OneShot[String]] =
      ConfigValue
        .default("my-super-secret-password")
        .as[Secret.OneShot[String]]
        .load[IO]

    result.flatMap(_.useAndDestroy(secretValue => {
      expect(secretValue == "my-super-secret-password")
    }))
  }

  test("Read Secret string with ciris") {

    val result: IO[Secret[String]] =
      ConfigValue
        .default("my-super-secret-password")
        .as[Secret[String]]
        .load[IO]

    result.flatMap(_.use(secretValue => {
      expect(secretValue == "my-super-secret-password")
    }))
  }
