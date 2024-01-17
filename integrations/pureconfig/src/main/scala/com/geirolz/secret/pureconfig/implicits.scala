package com.geirolz.secret.pureconfig

import _root_.pureconfig.ConfigReader
import com.geirolz.secret.{Secret, SecretStrategy}

given [T: ConfigReader: SecretStrategy]: ConfigReader[Secret[T]] =
  summon[ConfigReader[T]].map(t => Secret[T](t))
