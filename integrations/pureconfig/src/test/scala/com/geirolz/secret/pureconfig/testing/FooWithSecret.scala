package com.geirolz.secret.pureconfig.testing

import com.geirolz.secret.pureconfig.given
import com.geirolz.secret.Secret
import pureconfig.ConfigReader

case class FooWithSecret(
  bar: String,
  secret: Secret[String],
  oneShotSecret: Secret.OneShot[String]
) derives ConfigReader
