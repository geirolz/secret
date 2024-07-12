package com.geirolz.secret

import cats.effect.IO

class DeferredSecretCatsEffectSuite extends munit.CatsEffectSuite:

  test("DeferredSecret should be usable as resource") {
    DeferredSecret[IO, String](IO.pure("password")).resource
      .use(value =>
        IO(
          assertEquals(obtained = value, expected = "password")
        )
      )
  }

  test("DeferredSecret should be usable as resource directly") {
    DeferredSecret
      .resource[IO, String](IO.pure("password"))
      .use(value =>
        IO(
          assertEquals(obtained = value, expected = "password")
        )
      )
  }
