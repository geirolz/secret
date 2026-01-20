package com.geirolz.secret.ciris

import ciris.ConfigDecoder
import com.geirolz.secret.Secret
import com.geirolz.secret.strategy.SecretStrategy

given [A, T: ConfigDecoder[A, *]: SecretStrategy]: ConfigDecoder[A, Secret[T]] =
  summon[ConfigDecoder[A, T]].map(Secret(_))

given [A, T: ConfigDecoder[A, *]: SecretStrategy]: ConfigDecoder[A, Secret.OneShot[T]] =
  summon[ConfigDecoder[A, T]].map(Secret.oneShot(_))
