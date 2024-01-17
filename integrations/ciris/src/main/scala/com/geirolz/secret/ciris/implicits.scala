package com.geirolz.secret.ciris

import ciris.ConfigDecoder
import com.geirolz.secret.{Secret, SecretStrategy}

given [A, T: ConfigDecoder[A, *]: SecretStrategy]: ConfigDecoder[A, Secret[T]] =
  summon[ConfigDecoder[A, T]].map(Secret.apply)
