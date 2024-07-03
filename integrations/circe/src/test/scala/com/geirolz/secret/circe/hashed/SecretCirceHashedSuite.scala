package com.geirolz.secret.circe.hashed

import com.geirolz.secret.{secretTag, OneShotSecret, Secret}
import io.circe.Json
import io.circe.syntax.*

class SecretCirceHashedSuite extends munit.FunSuite:

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

  test("Secret should be encoded to json") {
    val secret: Secret[String] = Secret("secret_value")
    val result: Json           = secret.asJson

    assert(result == Json.fromString(secret.hashed))
  }

  test("OneShotSecret should be encoded to json") {
    val secret: OneShotSecret[String] = OneShotSecret("secret_value")
    val result: Json                  = secret.asJson

    assert(result == Json.fromString(secret.hashed))
  }
