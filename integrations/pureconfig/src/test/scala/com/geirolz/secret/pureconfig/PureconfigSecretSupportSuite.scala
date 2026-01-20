package com.geirolz.secret.pureconfig

import _root_.pureconfig.{ConfigReader, ConfigSource}
import _root_.pureconfig.backend.ConfigFactoryWrapper
import com.geirolz.secret.pureconfig.testing.FooWithSecret
import com.geirolz.secret.pureconfig.given
import com.geirolz.secret.Secret
import com.typesafe.config.Config
import weaver.*
import pureconfig.error.ConfigReaderFailures
import com.geirolz.secret.SecretDestroyed

object PureconfigSecretSupportSuite extends FunSuite:

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

    val fooWithSecret: FooWithSecret =
      ConfigSource
        .fromConfig(config)
        .loadOrThrow[FooWithSecret]

    val result1 = fooWithSecret.secret.euse(identity)
    val result2 = fooWithSecret.oneShotSecret.euseAndDestroy(identity)

    expect(result1 == Right("my-super-secret-password")) &&
    expect(result2 == Right("my-super-one-shot-secret-password"))
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

    val secretConfig: ConfigReader.Result[Secret.OneShot[String]] =
      summon[ConfigReader[Secret.OneShot[String]]]
        .from(config.getValue("conf.secret-value"))

    val result: Either[ConfigReaderFailures | SecretDestroyed, String] =
      secretConfig.flatMap(_.euseAndDestroy(identity))

    expect(result == Right("my-super-secret-password"))
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

    val configSecret: ConfigReader.Result[Secret[String]] =
      summon[ConfigReader[Secret[String]]]
        .from(config.getValue("conf.secret-value"))

    val result: Either[ConfigReaderFailures | SecretDestroyed, String] =
      configSecret.flatMap(_.euse(identity))

    expect(result == Right("my-super-secret-password"))
  }
