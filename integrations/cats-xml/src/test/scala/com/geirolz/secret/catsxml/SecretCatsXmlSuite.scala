package com.geirolz.secret.catsxml

import cats.xml.codec.Decoder.Result
import cats.xml.{Xml, XmlData}
import cats.xml.syntax.*
import com.geirolz.secret.{secretTag, Secret}

class SecretCatsXmlSuite extends munit.FunSuite:

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

    assert(result == Xml.string(secretTag))
  }

  test("Secret.OneShot should be encoded to json") {
    val secret: Secret.OneShot[String] = Secret.oneShot("secret_value")
    val result: Xml                    = secret.toXml

    assert(result == Xml.string(secretTag))
  }
