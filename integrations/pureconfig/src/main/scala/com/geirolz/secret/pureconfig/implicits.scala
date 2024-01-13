package com.geirolz.secret.pureconfig

import _root_.pureconfig.ConfigReader
import com.geirolz.secret.Secret

given [T: ConfigReader: Secret.Obfuscator]: ConfigReader[Secret[T]] =
  implicitly[ConfigReader[T]].map(t => Secret[T](t))
