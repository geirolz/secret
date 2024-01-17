package com.geirolz.secret.pureconfig.testing

import cats.Show
import pureconfig.ConfigReader

case class TestConfig(value: String)
object TestConfig:
  def defaultTest: TestConfig    = TestConfig("test_config")
  given Show[TestConfig]         = Show.fromToString
  given ConfigReader[TestConfig] = ConfigReader.fromCursor(_ => Right(defaultTest))
