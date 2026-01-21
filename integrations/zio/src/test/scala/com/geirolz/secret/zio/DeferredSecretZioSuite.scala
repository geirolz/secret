package com.geirolz.secret.zio

import com.geirolz.secret.*
import _root_.zio.*
import _root_.zio.interop.*
import _root_.zio.interop.catz.implicits.*
import weaver.*

object DeferredSecretZioSuite extends SimpleIOSuite:

  test("DeferredSecret should be usable as scoped") {
    ZIO
      .scoped {
        DeferredSecret
          .scoped[String](ZIO.succeed("password"))
          .flatMap(value =>
            ZIO.succeed(
              expect(value == "password")
            )
          )
      }
      .toEffect[cats.effect.IO]
  }

  test("DeferredSecret should be usable as scoped directly") {
    ZIO
      .scoped {
        DeferredSecret
          .scoped[String](ZIO.succeed("password"))
          .flatMap(value =>
            ZIO.succeed(
              expect(value == "password")
            )
          )
      }
      .toEffect[cats.effect.IO]
  }
