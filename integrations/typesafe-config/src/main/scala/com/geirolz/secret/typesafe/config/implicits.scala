package com.geirolz.secret.typesafe.config

import com.geirolz.secret.{ObfuscationStrategy, Secret}
import com.typesafe.config.Config

extension (config: Config)
  def getSecret[P: TypesafeConfigSecretAdapter: ObfuscationStrategy](path: String): Secret[P] =
    Secret(TypesafeConfigSecretAdapter[P].get(config, path))
