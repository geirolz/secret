package com.geirolz.secret.typesafe.config

import com.geirolz.secret.{OneShotSecret, Secret}
import com.geirolz.secret.strategy.SecretStrategy
import com.typesafe.config.Config

extension (config: Config)

  def getSecret[P: TypesafeConfigSecretAdapter: SecretStrategy](path: String): Secret[P] =
    Secret(TypesafeConfigSecretAdapter[P].get(config, path))

  def getOneShotSecret[P: TypesafeConfigSecretAdapter: SecretStrategy](path: String): OneShotSecret[P] =
    OneShotSecret(TypesafeConfigSecretAdapter[P].get(config, path))
