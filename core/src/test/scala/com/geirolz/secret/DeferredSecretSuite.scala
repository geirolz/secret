package com.geirolz.secret

import cats.effect.IO
import weaver.SimpleIOSuite

import scala.util.Try
import cats.syntax.all.*
import com.geirolz.secret.Secret.Deferred
import com.geirolz.secret.testing.*
import cats.effect.kernel.Ref

object DeferredSecretSuite extends SimpleIOSuite:

  test("Secret.Deferred should be evaluated every time use is called with Try") {
    for
      counterRef <- Ref.of[IO, Int](0)
      secret: Secret.Deferred[IO, Int] =
        Secret
          .deferred(counterRef.updateAndGet(_ + 1))
          .handleError(_ => Secret(0))
          .handleErrorWith(_ => IO(Secret(0)))

      result <- expect.allF[IO](
        // -1
        counterRef.get.map(v => expect(v == 0)),
        secret.use((v: Int) => expect(v == 1)),

        // -2
        counterRef.get.map(v => expect(v == 1)),
        secret.use((v: Int) => expect(v == 2)),

        // -3
        counterRef.get.map(v => expect(v == 2)),
        secret.evalUse((v: Int) => IO(expect(v == 3))),
        counterRef.get.map(v => expect(v == 3))
      )
    yield result
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
