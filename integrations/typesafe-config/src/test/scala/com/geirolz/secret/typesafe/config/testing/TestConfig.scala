package com.geirolz.secret.typesafe.config.testing

import cats.Show

case class TestConfig(value: String)
object TestConfig {

  def defaultTest: TestConfig = TestConfig("test_config")

  implicit val show: Show[TestConfig] = Show.fromToString
}
