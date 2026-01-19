package com.geirolz.secret.pureconfig

import _root_.pureconfig.{ConfigReader, ConfigSource}
import _root_.pureconfig.backend.ConfigFactoryWrapper
import com.geirolz.secret.pureconfig.testing.FooWithSecret
import com.geirolz.secret.pureconfig.given
import com.geirolz.secret.Secret
import com.typesafe.config.Config
import weaver.*

class PureconfigSecretSupportSuite extends SimpleIOSuite:

  pureTest("Read secrets with macro") {

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

    expect(result.secret.euse(v => v) == Right("my-super-secret-password"))
    expect(result.oneShotSecret.euseAndDestroy(v => v) == Right("my-super-one-shot-secret-password"))
  }

  pureTest("Read OneShotSecret string with pureconfig") {

    val config: Config = ConfigFactoryWrapper
      .parseString(
        """
        |conf {
        | secret-value: "my-super-secret-password"
        |}""".stripMargin
      )
      .toOption
      .get

    val result: ConfigReader.Result[Secret.OneShot[String]] =
      summon[ConfigReader[Secret.OneShot[String]]]
        .from(config.getValue("conf.secret-value"))

    assert(
      result
        .flatMap(_.euseAndDestroy(secretValue => {
          expect(secretValue == "my-super-secret-password")
        }))
        .isRight
    )
  }

  pureTest("Read Secret string with pureconfig") {

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
          expect(secretValue == "my-super-secret-password")
        }))
        .isRight
    )
  }
