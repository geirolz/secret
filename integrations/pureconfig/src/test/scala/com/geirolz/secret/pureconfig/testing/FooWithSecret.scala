package com.geirolz.secret.pureconfig.testing

import com.geirolz.secret.{OneShotSecret, SPassword}
import com.geirolz.secret.pureconfig.given
import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class FooWithSecret(
  bar: String,
  secret: SPassword,
  oneShotSecret: SPassword.OneShot
) derives ConfigReader
