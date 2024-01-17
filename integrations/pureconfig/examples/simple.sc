import com.geirolz.secret.Secret
import com.typesafe.config.Config
import pureconfig.ConfigReader
import pureconfig.backend.ConfigFactoryWrapper

import com.geirolz.secret.pureconfig.given

val config: Config = ConfigFactoryWrapper
  .parseString(
    """
      |conf {
      | secret-value: "my-super-secret-password"
      |}""".stripMargin
  )
  .toOption
  .get

val result: ConfigReader.Result[Secret[String]] = summon[ConfigReader[Secret[String]]].from(
  config.getValue("conf.secret-value")
)