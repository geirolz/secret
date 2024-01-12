package com.geirolz.secret.pureconfig.testing

import cats.Show
import pureconfig.ConfigReader

case class TestConfig(value: String)
object TestConfig {

  def defaultTest: TestConfig = TestConfig("test_config")

  implicit val show: Show[TestConfig]                 = Show.fromToString
  implicit val configReader: ConfigReader[TestConfig] = ConfigReader.fromCursor(_ => Right(defaultTest))
}
