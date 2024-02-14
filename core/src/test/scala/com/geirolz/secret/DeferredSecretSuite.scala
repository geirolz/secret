package com.geirolz.secret

import cats.effect.IO

import scala.util.Try

class DeferredSecretSuite extends munit.CatsEffectSuite:
  
  test("DeferredSecret should be evaluated every time use is called with IO") {
    var counter = 0
    val secret: DeferredSecret[IO, Int] =
      DeferredSecret(IO {
        counter += 1
        1
      }).map(_ + 1)
        .flatMap(v => DeferredSecret.pure(v + 1))
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

  test("DeferredSecret should be evaluated every time use is called with Try") {
    var counter = 0
    val secret: DeferredSecret[Try, Int] =
      DeferredSecret(Try {
        counter += 1
        1
      }).map(_ + 1)
        .flatMap(v => DeferredSecret.pure(v + 1))
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

  test("DeferredSecret.pure should always return the same value") {
    val secret: DeferredSecret[Try, String] = DeferredSecret.pure("hello")
    secret.use((v: String) => assertEquals(v, "hello"))
  }

  test("DeferredSecret.failure should always return a failure") {
    val ex                               = new Exception("error")
    val secret: DeferredSecret[Try, Int] = DeferredSecret.failed[Try, Int](ex)
    assert(secret.use(_ => ()).isFailure)
  }

  test("DeferredSecret.map should transform the value") {
    val secret: DeferredSecret[Try, Int] = DeferredSecret.pure(1)
    val result                           = secret.map(_ + 1)
    result.use((v: Int) => assertEquals(v, 2))
  }

  test("DeferredSecret.flatMap should transform the value") {
    val secret: DeferredSecret[Try, Int] = DeferredSecret.pure(1)
    val result                           = secret.flatMap(v => DeferredSecret.pure(v + 1))
    result.use((v: Int) => assertEquals(v, 2))
  }

  test("DeferredSecret.flatMap should propagate the failure") {
    val ex                               = new Exception("error")
    val secret: DeferredSecret[Try, Int] = DeferredSecret.failed[Try, Int](ex)
    val result                           = secret.flatMap(v => DeferredSecret.pure(v + 1))
    assert(result.use(_ => ()).isFailure)
  }
