package com.geirolz.secret.circe

import com.geirolz.secret.{OneShotSecret, Secret}
import io.circe.Json

class SecretCirceSuite extends munit.FunSuite:

  test("Secret should be decoded from json") {
    val json: Json = Json.fromString("secret_value")
    val result     = json.as[Secret[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }

  test("OneShotSecret should be decoded from json") {
    val json   = Json.fromString("secret_value")
    val result = json.as[OneShotSecret[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }
