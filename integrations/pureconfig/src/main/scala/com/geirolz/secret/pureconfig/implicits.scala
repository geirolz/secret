package com.geirolz.secret.pureconfig

import _root_.pureconfig.ConfigReader
import com.geirolz.secret.{SecretStrategy, Secret}

given [T: ConfigReader: SecretStrategy]: ConfigReader[Secret[T]] =
  summon[ConfigReader[T]].map(t => Secret[T](t))
