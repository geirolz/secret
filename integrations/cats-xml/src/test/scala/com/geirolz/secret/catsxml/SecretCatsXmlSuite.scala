package com.geirolz.secret.catsxml

import cats.xml.codec.Decoder.Result
import cats.xml.{Xml, XmlData}
import com.geirolz.secret.{OneShotSecret, Secret}

class SecretCatsXmlSuite extends munit.FunSuite:

  test("Secret should be decoded from xml") {
    val xml: XmlData.XmlString         = Xml.string("secret_value")
    val result: Result[Secret[String]] = xml.as[Secret[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }

  test("OneShotSecret should be decoded from xml") {
    val xml: XmlData.XmlString = Xml.string("secret_value")
    val result                 = xml.as[OneShotSecret[String]]

    result.toOption.get.euseAndDestroy(v => assert(v == "secret_value"))
  }
