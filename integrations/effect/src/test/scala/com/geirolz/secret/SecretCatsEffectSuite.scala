package com.geirolz.secret

import cats.effect.IO

class SecretCatsEffectSuite extends munit.CatsEffectSuite:

  test("Secret should be able to be used as a resource") {
    assertIO(
      Secret("password")
        .resource[IO]
        .use(IO(_)),
      "password"
    )
  }
