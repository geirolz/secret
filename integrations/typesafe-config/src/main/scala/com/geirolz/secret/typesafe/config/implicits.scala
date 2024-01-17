package com.geirolz.secret.typesafe.config

import com.geirolz.secret.{Secret, SecretStrategy}
import com.typesafe.config.Config

extension (config: Config)
  def getSecret[P: TypesafeConfigSecretAdapter: SecretStrategy](path: String): Secret[P] =
    Secret(TypesafeConfigSecretAdapter[P].get(config, path))
