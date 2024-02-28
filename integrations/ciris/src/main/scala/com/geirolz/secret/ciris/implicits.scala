package com.geirolz.secret.ciris

import ciris.ConfigDecoder
import com.geirolz.secret.{OneShotSecret, Secret}
import com.geirolz.secret.strategy.SecretStrategy

given [A, T: ConfigDecoder[A, *]: SecretStrategy]: ConfigDecoder[A, Secret[T]] =
  summon[ConfigDecoder[A, T]].map(Secret(_))

given [A, T: ConfigDecoder[A, *]: SecretStrategy]: ConfigDecoder[A, OneShotSecret[T]] =
  summon[ConfigDecoder[A, T]].map(OneShotSecret(_))
