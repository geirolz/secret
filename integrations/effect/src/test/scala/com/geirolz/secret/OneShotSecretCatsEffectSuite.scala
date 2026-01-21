package com.geirolz.secret

import cats.effect.IO
import weaver.*

object OneShotSecretCatsEffectSuite extends SimpleIOSuite:

  test("OneShotSecret should be usable as a resourceDestroy") {

    val secret1: Secret.OneShot[String] = Secret.oneShot("password")

    val test1: IO[Unit] =
      secret1
        .resourceDestroy[IO]
        .use(value => IO(expect(value == "password")))

    for {
      _   <- test1
      res <- IO(expect(secret1.isDestroyed))
    } yield res
  }

  test("Secret should be usable as a resource directly") {
    OneShotSecret
      .resource[IO, String]("password")
      .use(value => IO(expect(value == "password")))
  }
