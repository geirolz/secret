package com.geirolz.secret

import cats.effect.IO

class OneShotSecretCatsEffectSuite extends munit.CatsEffectSuite:

  test("OneShotSecret should be usable as a resourceDestroy") {

    val secret1: OneShotSecret[String] = OneShotSecret("password")

    val test1: IO[Unit] =
      secret1
        .resourceDestroy[IO]
        .use(value =>
          IO(
            assertEquals(obtained = value, expected = "password")
          )
        )

    for {
      _ <- test1
      _ <- IO(assert(secret1.isDestroyed))
    } yield ()
  }

  test("Secret should be usable as a resource directly") {
    OneShotSecret
      .resource[IO, String]("password")
      .use(value =>
        IO(
          assertEquals(obtained = value, expected = "password")
        )
      )
  }
