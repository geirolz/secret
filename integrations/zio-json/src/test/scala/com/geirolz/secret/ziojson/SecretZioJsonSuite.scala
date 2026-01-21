package com.geirolz.secret.ziojson

import com.geirolz.secret.{secretTag, Secret}
import _root_.zio.json.*
import weaver.*

object SecretZioJsonSuite extends SimpleIOSuite:

  pureTest("Secret should be decoded from json") {
    val json   = "\"secret_value\""
    val result = json.fromJson[Secret[String]]
    val e      = result.toOption.get.euseAndDestroy(v => v)

    expect(e == Right("secret_value"))
  }

  pureTest("Secret.OneShot should be decoded from json") {
    val json   = "\"secret_value\""
    val result = json.fromJson[Secret.OneShot[String]]
    val e      = result.toOption.get.euseAndDestroy(v => v)

    expect(e == Right("secret_value"))
  }

  pureTest("Secret should be encoded to json") {
    val secret: Secret[String] = Secret("secret_value")
    val result: String         = secret.toJson

    expect(result == s"\"$secretTag\"")
  }

  pureTest("Secret.OneShot should be encoded to json") {
    val secret: Secret.OneShot[String] = Secret.oneShot("secret_value")
    val result: String                 = secret.toJson

    expect(result == s"\"$secretTag\"")
  }
