package com.geirolz.secret.ciris

import com.geirolz.secret.Secret

import scala.util.Try
import cats.effect.IO
import ciris.ConfigValue

class CirisSecretSupportSuite extends munit.CatsEffectSuite:

  test("Read secret string with ciris") {

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
