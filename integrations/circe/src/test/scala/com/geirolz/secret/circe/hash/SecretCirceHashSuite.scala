package com.geirolz.secret.circe.hash

import com.geirolz.secret.{secretTag, Secret}
import io.circe.Json
import io.circe.syntax.*
import weaver.*

class SecretCirceHashSuite extends SimpleIOSuite:

  pureTest("Secret should be decoded from json") {
    val json: Json = Json.fromString("secret_value")
    val result     = json.as[Secret[String]]
    val e          = result.toOption.get.euseAndDestroy(v => v)

    expect(e == Right("secret_value"))
  }

  pureTest("Secret.OneShot should be decoded from json") {
    val json   = Json.fromString("secret_value")
    val result = json.as[Secret.OneShot[String]]
    val e      = result.toOption.get.euseAndDestroy(v => v)

    expect(e == Right("secret_value"))
  }

  pureTest("Secret should be encoded to json") {
    val secret: Secret[String] = Secret("secret_value")
    val result: Json           = secret.asJson

    expect(result == Json.fromString(secret.hash))
  }

  pureTest("Secret.OneShot should be encoded to json") {
    val secret: Secret.OneShot[String] = Secret.oneShot("secret_value")
    val result: Json                   = secret.asJson

    expect(result == Json.fromString(secret.hash))
  }
