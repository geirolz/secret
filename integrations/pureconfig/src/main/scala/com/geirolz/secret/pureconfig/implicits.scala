package com.geirolz.secret.pureconfig

import _root_.pureconfig.ConfigReader
import com.geirolz.secret.{OneShotSecret, Secret}
import com.geirolz.secret.strategy.SecretStrategy

given [T: ConfigReader: SecretStrategy]: ConfigReader[Secret[T]] =
  summon[ConfigReader[T]].map(t => Secret[T](t))

given [T: ConfigReader: SecretStrategy]: ConfigReader[OneShotSecret[T]] =
  summon[ConfigReader[T]].map(t => OneShotSecret[T](t))
