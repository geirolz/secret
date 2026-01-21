package com.geirolz.secret.catsxml.hash

import cats.xml.codec.Decoder.Result
import cats.xml.syntax.*
import cats.xml.{Xml, XmlData}
import com.geirolz.secret.{secretTag, Secret}
import weaver.*

object SecretCatsXmlHashSuite extends SimpleIOSuite:

  pureTest("Secret should be decoded from xml") {
    val xml: XmlData.XmlString         = Xml.string("secret_value")
    val result: Result[Secret[String]] = xml.as[Secret[String]]
    val e                              = result.toOption.get.euseAndDestroy(v => v)

    expect(e == Right("secret_value"))
  }

  pureTest("Secret.OneShot should be decoded from xml") {
    val xml: XmlData.XmlString = Xml.string("secret_value")
    val result                 = xml.as[Secret.OneShot[String]]
    val e                      = result.toOption.get.euseAndDestroy(v => v)

    expect(e == Right("secret_value"))
  }

  pureTest("Secret should be encoded to json") {
    val secret: Secret[String] = Secret("secret_value")
    val result: Xml            = secret.toXml

    expect(result == Xml.string(secret.hash))
  }

  pureTest("Secret.OneShot should be encoded to json") {
    val secret: Secret.OneShot[String] = Secret.oneShot("secret_value")
    val result: Xml                    = secret.toXml

    expect(result == Xml.string(secret.hash))
  }
