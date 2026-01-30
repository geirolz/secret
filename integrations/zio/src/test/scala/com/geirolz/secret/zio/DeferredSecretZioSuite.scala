package com.geirolz.secret.zio

import com.geirolz.secret.*
import _root_.zio.*
import _root_.zio.interop.*
import _root_.zio.interop.catz.implicits.*
import weaver.*

object DeferredSecretZioSuite extends SimpleIOSuite:

  test("DeferredSecret should be usable as scoped") {
    DeferredSecret
      .managed[String](ZIO.succeed("password"))
      .use(value =>
        ZIO.succeed(
          expect(value == "password")
        )
      )
      .toEffect[cats.effect.IO]
  }

  test("DeferredSecret should be usable as scoped directly") {
    DeferredSecret
      .managed[String](ZIO.succeed("password"))
      .use(value =>
        ZIO.succeed(
          expect(value == "password")
        )
      )
      .toEffect[cats.effect.IO]
  }
