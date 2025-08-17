package com.geirolz.secret.catsxml.hash

import cats.xml.codec.Decoder.Result
import cats.xml.syntax.*
import cats.xml.{Xml, XmlData}
import com.geirolz.secret.{secretTag, Secret}

class SecretCatsXmlHashSuite extends SimpleIOSuite:

  test("Secret should be decoded from xml") {
    val xml: XmlData.XmlString         = Xml.string("secret_value")
    val result: Result[Secret[String]] = xml.as[Secret[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }

  test("Secret.OneShot should be decoded from xml") {
    val xml: XmlData.XmlString = Xml.string("secret_value")
    val result                 = xml.as[Secret.OneShot[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }

  test("Secret should be encoded to json") {
    val secret: Secret[String] = Secret("secret_value")
    val result: Xml            = secret.toXml

    assert(result == Xml.string(secret.hash))
  }

  test("Secret.OneShot should be encoded to json") {
    val secret: Secret.OneShot[String] = Secret.oneShot("secret_value")
    val result: Xml                    = secret.toXml

    assert(result == Xml.string(secret.hash))
  }
