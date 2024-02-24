package com.geirolz.secret

import cats.effect.IO

class SecretCatsEffectSuite extends munit.CatsEffectSuite:
  
  test("Secret should be able to be used as a resource") {

    val secret1 = Secret("password")

    val test1: IO[Unit] =
      secret1
        .resource[IO]
        .use(value =>
          IO(
            assertEquals(obtained = value, expected = "password")
          )
        )

    for {
      _ <- test1
      _ <- IO(assert(!secret1.isDestroyed))
    } yield ()
  }
