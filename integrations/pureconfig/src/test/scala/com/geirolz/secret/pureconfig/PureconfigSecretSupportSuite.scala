package com.geirolz.secret.pureconfig

import _root_.pureconfig.ConfigReader
import _root_.pureconfig.backend.ConfigFactoryWrapper
import com.geirolz.secret.Secret
import com.typesafe.config.Config

class PureconfigSecretSupportSuite extends munit.FunSuite:

  test("Read secret string with pureconfig") {

    val config: Config = ConfigFactoryWrapper
      .parseString(
        """
        |conf {
        | secret-value: "my-super-secret-password"
        |}""".stripMargin
      )
      .toOption
      .get

    val result: ConfigReader.Result[Secret[String]] = summon[ConfigReader[Secret[String]]].from(
      config.getValue("conf.secret-value")
    )

    assert(
      result
        .flatMap(_.useE(secretValue => {
          assertEquals(
            obtained = secretValue,
            expected = "my-super-secret-password"
          )
        }))
        .isRight
    )

  }
