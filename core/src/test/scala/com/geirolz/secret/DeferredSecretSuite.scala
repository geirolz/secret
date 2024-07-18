package com.geirolz.secret

import cats.effect.IO

import scala.util.Try

class DeferredSecretSuite extends munit.CatsEffectSuite:

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

    assert(counter == 0)
    secret.use((v: Int) => assertEquals(v, 3)).unsafeRunSync()
    assert(counter == 1)
    secret.use((v: Int) => assertEquals(v, 3)).unsafeRunSync()
    assert(counter == 2)
    secret.evalUse((v: Int) => IO(assertEquals(v, 3))).unsafeRunSync()
    assert(counter == 3)
  }

  test("Secret.Deferred should be evaluated every time use is called with Try") {
    var counter = 0
    val secret: Secret.Deferred[Try, Int] =
      Secret
        .deferred(Try {
          counter += 1
          1
        })
        .map(_ + 1)
        .flatMap(v => Secret.deferred.pure(v + 1))
        .handleError(_ => Secret(0))
        .handleErrorWith(_ => Try(Secret(0)))

    assert(counter == 0)
    assert(secret.use((v: Int) => assertEquals(v, 3)).isSuccess)
    assert(counter == 1)
    assert(secret.use((v: Int) => assertEquals(v, 3)).isSuccess)
    assert(counter == 2)
    assert(secret.evalUse((v: Int) => Try(assertEquals(v, 3))).isSuccess)
    assert(counter == 3)
  }

  test("Secret.deferred.pure should always return the same value") {
    val secret: Secret.Deferred[Try, String] = Secret.deferred.pure("hello")
    secret.use((v: String) => assertEquals(v, "hello"))
  }

  test("Secret.deferred.failure should always return a failure") {
    val ex                                = new Exception("error")
    val secret: Secret.Deferred[Try, Int] = Secret.deferred.failed[Try, Int](ex)
    assert(secret.use(_ => ()).isFailure)
  }

  test("Secret.deferred.map should transform the value") {
    val secret: Secret.Deferred[Try, Int] = Secret.deferred.pure(1)
    val result                            = secret.map(_ + 1)
    result.use((v: Int) => assertEquals(v, 2))
  }

  test("Secret.deferred.flatMap should transform the value") {
    val secret: Secret.Deferred[Try, Int] = Secret.deferred.pure(1)
    val result                            = secret.flatMap(v => Secret.deferred.pure(v + 1))
    result.use((v: Int) => assertEquals(v, 2))
  }

  test("Secret.deferred.flatMap should propagate the failure") {
    val ex                                = new Exception("error")
    val secret: Secret.Deferred[Try, Int] = Secret.deferred.failed[Try, Int](ex)
    val result                            = secret.flatMap(v => Secret.deferred.pure(v + 1))
    assert(result.use(_ => ()).isFailure)
  }
