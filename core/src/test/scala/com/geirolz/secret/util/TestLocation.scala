package com.geirolz.secret.util

import weaver.SimpleIOSuite

object TestLocation extends SimpleIOSuite:

  pureTest("Test Location") {

    val location: Location = summon[Location]

    expect(
      location.sourceFile.substring(location.sourceFile.lastIndexOf("/") + 1)
        == "TestLocation.scala"
    ) &&
    expect(location.line == 7) &&
    expect(location.column == 45)
  }
