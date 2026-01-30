package com.geirolz.secret.zio

import com.geirolz.secret.*
import _root_.zio.ZIO
import _root_.zio.interop.*
import _root_.zio.interop.catz.implicits.*
import weaver.*
import scala.language.implicitConversions

object SecretZioSuite extends SimpleIOSuite:

  test("Secret should be usable as scoped") {
    val secret1: Secret[String] = Secret("password")
    secret1.managed
      .use(value => ZIO.succeed(expect(value == "password")))
      .as(expect(!secret1.isDestroyed))
      .mapError(identity[Throwable])
      .toEffect[cats.effect.IO]
  }

  test("Secret should be usable as scopedDestroy") {
    val secret1: Secret[String] = Secret("password")
    secret1.managedDestroy
      .use(value => ZIO.succeed(expect(value == "password")))
      .as(expect(secret1.isDestroyed))
      .toEffect[cats.effect.IO]
  }

  test("Secret should be usable as scoped directly") {
    Secret
      .managed[String]("password")
      .use(value => ZIO.succeed(expect(value == "password")))
      .toEffect[cats.effect.IO]
  }
