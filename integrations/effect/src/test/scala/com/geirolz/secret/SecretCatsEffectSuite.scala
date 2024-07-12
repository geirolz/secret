package com.geirolz.secret

import cats.effect.IO

class SecretCatsEffectSuite extends munit.CatsEffectSuite:

  test("Secret should be usable as a resource") {

    val secret1: Secret[String] = Secret("password")

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

  test("Secret should be usable as a resourceDestroy") {

    val secret1: Secret[String] = Secret("password")

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
    Secret
      .resource[IO, String]("password")
      .use(value =>
        IO(
          assertEquals(obtained = value, expected = "password")
        )
      )
  }
