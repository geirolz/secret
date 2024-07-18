package com.geirolz.secret.circe

import com.geirolz.secret.{secretTag, Secret}
import io.circe.Json
import io.circe.syntax.*

class SecretCirceSuite extends munit.FunSuite:

  test("Secret should be decoded from json") {
    val json: Json = Json.fromString("secret_value")
    val result     = json.as[Secret[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }

  test("Secret.OneShot should be decoded from json") {
    val json   = Json.fromString("secret_value")
    val result = json.as[Secret.OneShot[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }

  test("Secret should be encoded to json") {
    val secret: Secret[String] = Secret("secret_value")
    val result: Json           = secret.asJson

    assert(result == Json.fromString(secretTag))
  }

  test("Secret.OneShot should be encoded to json") {
    val secret: Secret.OneShot[String] = Secret.oneShot("secret_value")
    val result: Json                   = secret.asJson

    assert(result == Json.fromString(secretTag))
  }
