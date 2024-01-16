package com.geirolz.secret.pureconfig

import _root_.pureconfig.ConfigReader
import com.geirolz.secret.{ObfuscationStrategy, Secret}

given [T: ConfigReader: ObfuscationStrategy]: ConfigReader[Secret[T]] =
  implicitly[ConfigReader[T]].map(t => Secret[T](t))