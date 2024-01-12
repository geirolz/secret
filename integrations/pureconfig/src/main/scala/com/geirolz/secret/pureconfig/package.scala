package com.geirolz.secret

import _root_.pureconfig.ConfigReader

package object pureconfig {

  implicit def configReaderForSecret[T: ConfigReader: Secret.Obfuser]: ConfigReader[Secret[T]] =
    implicitly[ConfigReader[T]].map(t => Secret[T](t))
}
