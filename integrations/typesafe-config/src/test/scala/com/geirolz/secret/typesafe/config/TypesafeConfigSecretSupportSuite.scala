package com.geirolz.secret.typesafe.config

import com.geirolz.secret.Secret
import com.typesafe.config.{Config, ConfigFactory}
import weaver.*

class TypesafeConfigSecretSupportSuite extends SimpleIOSuite:

  pureTest("Read OneShotSecret string with typesafe config") {

    val config: Config = ConfigFactory.parseString(
      """
          |conf {
          | secret-value: "my-super-secret-password"
          |}""".stripMargin
    )

    val result: Secret.OneShot[String] = config.getOneShotSecret[String]("conf.secret-value")

    assert(
      result
        .euseAndDestroy(secretValue => {
          expect(secretValue == "my-super-secret-password")
        })
        .isRight
    )
  }

  pureTest("Read Secret string with typesafe config") {

    val config: Config = ConfigFactory.parseString(
      """
          |conf {
          | secret-value: "my-super-secret-password"
          |}""".stripMargin
    )

    val result: Secret[String] = config.getSecret[String]("conf.secret-value")

    assert(
      result
        .euse(secretValue => {
          expect(secretValue == "my-super-secret-password")
        })
        .isRight
    )
  }
