//package com.geirolz.secret.pureconfig
//
//import _root_.pureconfig.ConfigReader
//import _root_.pureconfig.backend.ConfigFactoryWrapper
//import com.geirolz.secret.Secret
//import com.typesafe.config.Config
//
//import scala.util.Try
//
//class CirisSecretSupportSuite extends munit.FunSuite:
//
//  import ciris._
//
//  test("Read secret string with pureconfig") {
//
//    val result: Try[Any] = prop("").as[Secret[String]].load[Try]
//
////    val config: Config = ConfigFactoryWrapper
////      .parseString(
////        """
////        |conf {
////        | secret-value: "my-super-secret-password"
////        |}""".stripMargin
////      )
////      .toOption
////      .get
//
////    val result: ConfigReader.Result[Secret[String]] = implicitly[ConfigReader[Secret[String]]].from(
////      config.getValue("conf.secret-value")
////    )
//
//    assert(
//      result
//        .flatMap(_.useE(secretValue => {
//          assertEquals(
//            obtained = secretValue,
//            expected = "my-super-secret-password"
//          )
//        }))
//        .isRight
//    )
//
//  }
