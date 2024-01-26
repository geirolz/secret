import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.{KeyValueBuffer, Secret}

given SecretStrategy[String] = SecretStrategy[String](
  Obfuscator.of[String](_ => KeyValueBuffer.directEmpty(0)),
  DeObfuscator.of[String](_ => "CUSTOM"),
)

Secret("my_password").useE(secret => secret)