package com.geirolz.secret.ciris

import ciris.ConfigDecoder
import com.geirolz.secret.{ObfuscationStrategy, Secret}

given [F <: Nothing, T: ConfigDecoder[F, *]: ObfuscationStrategy]: ConfigDecoder[F, Secret[T]] =
  summon[ConfigDecoder[F, T]].map(Secret.apply)
