# Secret

[![Build Status](https://github.com/geirolz/secret/actions/workflows/cicd.yml/badge.svg)](https://github.com/geirolz/secret/actions)
[![codecov](https://img.shields.io/codecov/c/github/geirolz/secret)](https://codecov.io/gh/geirolz/secret)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/db3274b55e0c4031803afb45f58d4413)](https://www.codacy.com/manual/david.geirola/secret?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=geirolz/secret&amp;utm_campaign=Badge_Grade)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/com.github.geirolz/secret_3?server=https%3A%2F%2Foss.sonatype.org)](https://mvnrepository.com/artifact/com.github.geirolz/secret)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://api.mergify.com/v1/badges/geirolz/secret&style=flat)](https://mergify.io)
[![GitHub license](https://img.shields.io/github/license/geirolz/secret)](https://github.com/geirolz/secret/blob/main/LICENSE)

A Scala 3, functional, type-safe and memory-safe library to handle secret values

`Secret` library does the best to avoid leaking information in memory and in the code BUT an attack is always possible and I don't give any certainties or
guarantees about security using this library, you use it at your own risk. The code is open sourced, you can check the implementation and take your
decision consciously. I'll do my best to improve the security and the documentation of this project.

Please, drop a ⭐️ if you are interested in this project and you want to support it.

Scala 3 only, Scala 2 is not supported.
```sbt
libraryDependencies += "com.github.geirolz" %% "secret" % "0.0.4"
```

## Obfuscation

By default the value is obfuscated when creating the `Secret` instance using the implicit `SecretStrategy` which, by default, transform the value into a xor-ed
`ByteBuffer` which store bytes outside the JVM using direct memory access.

The obfuscated value is de-obfuscated using the implicit `SecretStrategy` instance every time `use`, and derived method, are invoked which returns the original
value converting the bytes back to `T` re-applying the xor formula.

## API and Type safety

While obfuscating the value prevents or at least makes it harder to read the value from the memory, `Secret` class API are designed to avoid leaking
information in other ways. Preventing developers to improperly use the secret value ( logging, etc...).

Example
```scala
import com.geirolz.secret.*
import scala.util.Try

case class Database(password: String)

val secretString: Secret[String]  = Secret("password")
// secretString: Secret[String] = ** SECRET **
val database: Either[SecretDestroyed, Database]       = secretString.useAndDestroyE(password => Database(password))
// database: Either[SecretDestroyed, Database] = Right(
//   value = Database(password = "password")
// )

// if you try to access the secret value once used, you'll get an error
secretString.useE(println(_))
// res1: Either[SecretDestroyed, Unit] = Left(
//   value = SecretDestroyed(destroyedAt = README.md:25:119)
// )
```

### Integrations

These integrations aim to enhance the functionality and capabilities of `Secret` type making it easier to use in different contexts.

#### Cats Effect
```sbt
libraryDependencies += "com.github.geirolz" %% "secret-effect" % "0.0.4"
```

```scala
import com.geirolz.secret.*
import cats.effect.{IO, Resource}

val res: Resource[IO, String] = Secret("password").resource[IO]
```

#### Pureconfig
```sbt
libraryDependencies += "com.github.geirolz" %% "secret-pureconfig" % "0.0.4"
```

Just provides the `ConfigReader` instance for `Secret[T]` type.
There must be an `ConfigReader[T]` and a `SecretStrategy[T]` instances implicitly in the scope.
```scala
import com.geirolz.secret.pureconfig.given
```
#### Typesafe Config
```sbt
libraryDependencies += "com.github.geirolz" %% "secret-typesafe-config" % "0.0.4"
```
```scala
import com.geirolz.secret.typesafe.config.given
```

#### Ciris
```sbt
libraryDependencies += "com.github.geirolz" %% "secret-ciris" % "0.0.4"
```
```scala
import com.geirolz.secret.ciris.given
```

## Adopters

If you are using Secret in your company, please let me know and I'll add it to the list! It means a lot to me.

## Custom Obfuscation Strategy for a specific type

If you want to use a custom obfuscation strategy for a specific type you can implement a custom `SecretStrategy` and provide an implicit instance of it during the secret creation.
If you think that your strategy can be useful for other people, please consider to contribute to the project and add it to the library.

```scala
import com.geirolz.secret.strategy.SecretStrategy
import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.{KeyValueBuffer, Secret}

given SecretStrategy[String] = SecretStrategy[String](
   Obfuscator.of[String](_ => KeyValueBuffer.directEmpty(0)),
   DeObfuscator.of[String](_ => "CUSTOM"),
)

Secret("my_password").useE(secret => secret)
// res7: Either[SecretDestroyed, String] = Right(value = "CUSTOM")
```

## Custom Obfuscation Strategy algebra

If you want to use a custom obfuscation strategy algebra you can implement a custom `SecretStrategyAlgebra` and provide an implicit `SecretStrategyFactory` instance built on it during the secret creation.
If you think that your strategy can be useful for other people, please consider to contribute to the project and add it to the library.

```scala
import com.geirolz.secret.strategy.SecretStrategy.{DeObfuscator, Obfuscator}
import com.geirolz.secret.strategy.{SecretStrategy, SecretStrategyAlgebra}
import com.geirolz.secret.{KeyValueBuffer, PlainValueBuffer, Secret}

import java.nio.ByteBuffer

// build the custom algebra
val myCustomAlgebra = new SecretStrategyAlgebra:
final def obfuscator[P](f: P => PlainValueBuffer): Obfuscator[P] =
   Obfuscator.of { (plain: P) => KeyValueBuffer(ByteBuffer.allocateDirect(0), f(plain)) }

final def deObfuscator[P](f: PlainValueBuffer => P): DeObfuscator[P] =
   DeObfuscator.of { bufferTuple => f(bufferTuple.roObfuscatedBuffer) }
// myCustomAlgebra: SecretStrategyAlgebra = repl.MdocSession$MdocApp8$$anon$6@d448e97

// build factory based on the algebra
val myCustomStrategyFactory = myCustomAlgebra.newFactory
// myCustomStrategyFactory: SecretStrategyFactory = com.geirolz.secret.strategy.SecretStrategyFactory@2729ee45

// ----------------------------- USAGE -----------------------------
// implicitly in the scope
import myCustomStrategyFactory.given
Secret("my_password").useE(secret => secret)
// res9: Either[SecretDestroyed, String] = Right(value = "my_password")

// or restricted to a specific scope
myCustomStrategyFactory {
   Secret("my_password").useE(secret => secret)
}
// res10: Either[SecretDestroyed, String] = Right(value = "my_password")
```

## Contributing

We welcome contributions from the open-source community to make Secret even better. If you have any bug reports,
feature requests, or suggestions, please submit them via GitHub issues. Pull requests are also welcome.

Before contributing, please read
our [Contribution Guidelines](https://github.com/geirolz/secret/blob/main/CONTRIBUTING.md) to understand the
development process and coding conventions.

Please remember te following:

- Run `sbt scalafmtAll` before submitting a PR.
- Run `sbt gen-doc` to update the documentation.

## License

Secret is released under the [Apache License 2.0](https://github.com/geirolz/secret/blob/main/LICENSE).
Feel free to use it in your open-source or commercial projects.

## Acknowledgements
- https://westonal.medium.com/protecting-strings-in-jvm-memory-84c365f8f01c
- VisualVM
- ChatGPT
- Personal experience in companies where I worked
