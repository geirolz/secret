package com.geirolz.secret

import cats.effect.IO
import weaver.SimpleIOSuite

import scala.util.Try
import cats.syntax.all.*
import com.geirolz.secret.Secret.Deferred
import com.geirolz.secret.testing.*

object DeferredSecretSuite extends SimpleIOSuite:

  test("Secret.Deferred should be evaluated every time use is called with IO") {
    var counter = 0
    val secret: Secret.Deferred[IO, Int] =
      Secret
        .deferred(IO {
          counter += 1
          1
        })
        .map(_ + 1)
        .flatMap(v => Secret.deferred.pure(v + 1))
        .handleError(_ => Secret(0))
        .handleErrorWith(_ => IO(Secret(0)))

    expect.allF[IO](
      expect(counter == 0).pure[IO],
      secret.use((v: Int) => expect(v == 3)),
      expect(counter == 1).pure[IO],
      secret.use((v: Int) => expect(v == 3)),
      expect(counter == 2).pure[IO],
      secret.evalUse((v: Int) => IO(expect(v == 3))),
      expect(counter == 3).pure[IO]
    )
  }

  test("Secret.Deferred should be evaluated every time use is called with Try") {
    var counter = 0
    val secret: Secret.Deferred[IO, Int] =
      Secret
        .deferred(IO {
          counter += 1
          1
        })
        .map(_ + 1)
        .flatMap(v => Secret.deferred.pure(v + 1))
        .handleError(_ => Secret(0))
        .handleErrorWith(_ => IO(Secret(0)))

    expect.allF[IO](
      expect(counter == 0).pure[IO],
      secret.use((v: Int) => expect(v == 3)),
      expect(counter == 1).pure[IO],
      secret.use((v: Int) => expect(v == 3)),
      expect(counter == 2).pure[IO],
      secret.evalUse((v: Int) => IO(expect(v == 3))),
      expect(counter == 3).pure[IO]
    )
  }

  test("Secret.deferred.pure should always return the same value") {
    val secret: Secret.Deferred[IO, String] = Secret.deferred.pure("hello")
    secret.use((v: String) => expect(v == "hello"))
  }

  test("Secret.deferred.failure should always return a failure") {
    val ex: Exception                    = new Exception("error")
    val secret: Secret.Deferred[IO, Int] = Secret.deferred.failed[IO, Int](ex)
    secret.use(_ => ()).attempt.map(v => expect(v.isLeft))
  }

  test("Secret.deferred.map should transform the value") {
    val secret: Secret.Deferred[IO, Int] = Secret.deferred.pure(1)
    val result: Secret.Deferred[IO, Int] = secret.map(_ + 1)
    result.use((v: Int) => expect(v == 2))
  }

  test("Secret.deferred.flatMap should transform the value") {
    val secret: Secret.Deferred[IO, Int] = Secret.deferred.pure(1)
    val result: Secret.Deferred[IO, Int] = secret.flatMap(v => Secret.deferred.pure(v + 1))
    result.use((v: Int) => expect(v == 2))
  }

  test("Secret.deferred.flatMap should propagate the failure") {
    val ex: Exception                    = new Exception("error")
    val secret: Secret.Deferred[IO, Int] = Secret.deferred.failed[IO, Int](ex)
    val result: Secret.Deferred[IO, Int] = secret.flatMap(v => Secret.deferred.pure(v + 1))
    result.use(_ => ()).attempt.map(v => expect(v.isLeft))
  }
