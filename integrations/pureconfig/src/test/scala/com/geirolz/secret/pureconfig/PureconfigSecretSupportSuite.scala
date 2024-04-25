package com.geirolz.secret.pureconfig

import _root_.pureconfig.{ConfigReader, ConfigSource}
import _root_.pureconfig.backend.ConfigFactoryWrapper
import com.geirolz.secret.pureconfig.testing.FooWithSecret
import com.geirolz.secret.pureconfig.given
import com.geirolz.secret.{OneShotSecret, SPassword, Secret}
import com.typesafe.config.Config

class PureconfigSecretSupportSuite extends munit.FunSuite:

  test("Read secrets with macro") {

    val config: Config = ConfigFactoryWrapper
      .parseString(
        """
          |{
          | bar: "bar"
          | secret: "my-super-secret-password"
          | one-shot-secret: "my-super-one-shot-secret-password"
          |}""".stripMargin
      )
      .toOption
      .get

    val result: FooWithSecret = ConfigSource.fromConfig(config).loadOrThrow[FooWithSecret]
    result.secret.euse(secretValue => {
      assertEquals(
        obtained = secretValue,
        expected = "my-super-secret-password"
      )
    })

    result.oneShotSecret.euseAndDestroy(secretValue => {
      assertEquals(
        obtained = secretValue,
        expected = "my-super-one-shot-secret-password"
      )
    })

  }

  test("Read OneShotSecret string with pureconfig") {

    val config: Config = ConfigFactoryWrapper
      .parseString(
        """
        |conf {
        | secret-value: "my-super-secret-password"
        |}""".stripMargin
      )
      .toOption
      .get

    val result: ConfigReader.Result[OneShotSecret[String]] =
      summon[ConfigReader[OneShotSecret[String]]]
        .from(config.getValue("conf.secret-value"))

    assert(
      result
        .flatMap(_.euseAndDestroy(secretValue => {
          assertEquals(
            obtained = secretValue,
            expected = "my-super-secret-password"
          )
        }))
        .isRight
    )
  }

  test("Read Secret string with pureconfig") {

    val config: Config = ConfigFactoryWrapper
      .parseString(
        """
          |conf {
          | secret-value: "my-super-secret-password"
          |}""".stripMargin
      )
      .toOption
      .get

    val result: ConfigReader.Result[Secret[String]] =
      summon[ConfigReader[Secret[String]]]
        .from(config.getValue("conf.secret-value"))

    assert(
      result
        .flatMap(_.euse(secretValue => {
          assertEquals(
            obtained = secretValue,
            expected = "my-super-secret-password"
          )
        }))
        .isRight
    )
  }
