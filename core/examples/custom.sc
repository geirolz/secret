import com.geirolz.secret.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.{KeyValueBuffer, Secret, SecretStrategy}

given SecretStrategy[String] = SecretStrategy[String](
  Obfuscator[String](_ => KeyValueBuffer.directEmpty(0)),
  DeObfuscator[String](_ => "CUSTOM"),
)

Secret("my_password").useE(secret => secret)