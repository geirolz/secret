package com.geirolz.secret

import cats.effect.IO
import weaver.*

object DeferredSecretCatsEffectSuite extends SimpleIOSuite:

  test("DeferredSecret should be usable as resource") {
    DeferredSecret[IO, String](IO.pure("password")).resource
      .use(value =>
        IO(
          expect(value == "password")
        )
      )
  }

  test("DeferredSecret should be usable as resource directly") {
    DeferredSecret
      .resource[IO, String](IO.pure("password"))
      .use(value =>
        IO(
          expect(value == "password")
        )
      )
  }
