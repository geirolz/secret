package com.geirolz.secret

import cats.effect.IO
import weaver.{*, given}

object SecretCatsEffectSuite extends SimpleIOSuite:

  test("Secret should be usable as a resource") {

    val secret1: Secret[String] = Secret("password")
    val test1: IO[Unit] =
      secret1
        .resource[IO]
        .use(value =>
          IO(
            expect(value == "password")
          )
        )

    for {
      _   <- test1
      res <- IO(assert(!secret1.isDestroyed))
    } yield res
  }

  test("Secret should be usable as a resourceDestroy") {

    val secret1: Secret[String] = Secret("password")
    val test1: IO[Unit] =
      secret1
        .resourceDestroy[IO]
        .use(value => IO(expect(value == "password")))

    for {
      _   <- test1
      res <- IO(assert(secret1.isDestroyed))
    } yield res
  }

  test("Secret should be usable as a resource directly") {
    Secret
      .resource[IO, String]("password")
      .use(value =>
        IO(
          expect(value == "password")
        )
      )
  }
