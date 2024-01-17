package com.geirolz.secret.typesafe.config

import com.geirolz.secret.{SecretStrategy, Secret}
import com.typesafe.config.Config

extension (config: Config)
  def getSecret[P: TypesafeConfigSecretAdapter: SecretStrategy](path: String): Secret[P] =
    Secret(TypesafeConfigSecretAdapter[P].get(config, path))
