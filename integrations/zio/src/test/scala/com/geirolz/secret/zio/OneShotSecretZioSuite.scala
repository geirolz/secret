package com.geirolz.secret.zio

import com.geirolz.secret.*
import _root_.zio.ZIO
import _root_.zio.interop.*
import _root_.zio.interop.catz.implicits.*
import weaver.*
import scala.language.implicitConversions

object OneShotSecretZioSuite extends SimpleIOSuite:

  test("OneShotSecret should be usable as scopedDestroy") {
    val secret1: Secret.OneShot[String] = Secret.oneShot("password")
    secret1.managedDestroy
      .use(value => ZIO.succeed(expect(value == "password")))
      .as(expect(secret1.isDestroyed))
      .toEffect[cats.effect.IO]
  }

  test("OneShotSecret should be usable as scoped directly") {
    OneShotSecret
      .managed[String]("password")
      .use(value => ZIO.succeed(expect(value == "password")))
      .toEffect[cats.effect.IO]
  }
