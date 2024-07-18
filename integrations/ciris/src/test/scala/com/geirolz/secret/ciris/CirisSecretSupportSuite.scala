package com.geirolz.secret.ciris

import cats.effect.IO
import ciris.ConfigValue
import com.geirolz.secret.Secret

class CirisSecretSupportSuite extends munit.CatsEffectSuite:

  test("Read OneShotSecret string with ciris") {

    val result: IO[Secret.OneShot[String]] =
      ConfigValue
        .default("my-super-secret-password")
        .as[Secret.OneShot[String]]
        .load[IO]

    assertIO_(
      result.flatMap(_.useAndDestroy(secretValue => {
        assertEquals(
          obtained = secretValue,
          expected = "my-super-secret-password"
        )
      }))
    )
  }

  test("Read Secret string with ciris") {

    val result: IO[Secret[String]] =
      ConfigValue
        .default("my-super-secret-password")
        .as[Secret[String]]
        .load[IO]

    assertIO_(
      result.flatMap(_.use(secretValue => {
        assertEquals(
          obtained = secretValue,
          expected = "my-super-secret-password"
        )
      }))
    )

  }
