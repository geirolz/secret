package com.geirolz.secret.typesafe.config

import com.geirolz.secret.Secret
import com.typesafe.config.{Config, ConfigFactory}

class TypesafeConfigSecretSupportSuite extends munit.FunSuite:

  test("Read OneShotSecret string with typesafe config") {

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
          assertEquals(
            obtained = secretValue,
            expected = "my-super-secret-password"
          )
        })
        .isRight
    )
  }

  test("Read Secret string with typesafe config") {

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
          assertEquals(
            obtained = secretValue,
            expected = "my-super-secret-password"
          )
        })
        .isRight
    )
  }
