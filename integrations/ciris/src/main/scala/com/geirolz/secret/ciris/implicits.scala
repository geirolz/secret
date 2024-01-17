package com.geirolz.secret.ciris

import ciris.ConfigDecoder
import com.geirolz.secret.{SecretStrategy, Secret}

given [F <: Nothing, T: ConfigDecoder[F, *]: SecretStrategy]: ConfigDecoder[F, Secret[T]] =
  summon[ConfigDecoder[F, T]].map(Secret.apply)
