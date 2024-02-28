package com.geirolz.secret.ciris

import com.geirolz.secret.{OneShotSecret, Secret}

import scala.util.Try
import cats.effect.IO
import ciris.ConfigValue

class CirisSecretSupportSuite extends munit.CatsEffectSuite:

  test("Read OneShotSecret string with ciris") {

    val result: IO[OneShotSecret[String]] =
      ConfigValue
        .default("my-super-secret-password")
        .as[OneShotSecret[String]]
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
