package com.geirolz.secret

class VisualVmTest extends munit.FunSuite {

  test("test") {

    val secret = Secret("TEST")
    println(secret)

    while (true) {
      Thread.sleep(1000)
    }
  }

}
