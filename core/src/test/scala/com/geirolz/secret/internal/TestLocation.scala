package com.geirolz.secret.internal

class TestLocation extends munit.FunSuite {

  test("Test Location") {

    val location: Location = summon[Location]

    assertEquals(
      obtained = location.sourceFile.substring(location.sourceFile.lastIndexOf("/") + 1),
      expected = "TestLocation.scala"
    )
    assertEquals(
      obtained = location.line,
      expected = 7
    )
    assertEquals(
      obtained = location.column,
      expected = 45
    )
  }
}
