import sbt.*
import scala.language.postfixOps

object ProjectDependencies {

  // base
  private val catsVersion   = "2.13.0"
  private val bcryptVersion = "0.10.2"
  // test
  private val weaverVersion = "0.8.4"
  private val scalacheck    = "1.19.0"
  // integrations
  private val catsEffectVersion     = "3.7-4972921"
  private val pureConfigVersion     = "0.17.9"
  private val typesafeConfigVersion = "1.4.5"
  private val cirisVersion          = "3.8.0"

  lazy val common: Seq[ModuleID] = Seq(
    // base
    "org.typelevel" %% "cats-core" % catsVersion,
    "at.favre.lib"   % "bcrypt"    % bcryptVersion,

    // test
    "org.typelevel"       %% "cats-effect"       % catsEffectVersion % Test,
    "com.disneystreaming" %% "weaver-cats"       % weaverVersion     % Test,
    "com.disneystreaming" %% "weaver-scalacheck" % weaverVersion     % Test,
    "org.scalacheck"      %% "scalacheck"        % scalacheck        % Test
  )

  object Core {
    lazy val dedicated: Seq[ModuleID] = Nil
  }

  object Modules {
    object Encrypt {
      lazy val dedicated: Seq[ModuleID] = List(
      )
    }
  }

  object Integrations {

    object CatsEffect {
      lazy val dedicated: Seq[ModuleID] = List(
        "org.typelevel" %% "cats-effect" % catsEffectVersion
      )
    }

    object Pureconfig {
      lazy val dedicated: Seq[ModuleID] = List(
        "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion
      )
    }

    object TypesafeConfig {
      lazy val dedicated: Seq[ModuleID] = List(
        "com.typesafe" % "config" % typesafeConfigVersion
      )
    }

    object Ciris {
      lazy val dedicated: Seq[ModuleID] = List(
        "is.cir"        %% "ciris"             % cirisVersion,
        "org.typelevel" %% "cats-effect"       % catsEffectVersion % Test,
      )
    }

    object Circe {
      lazy val dedicated: Seq[ModuleID] = List(
        "io.circe" %% "circe-core" % "0.14.15"
      )
    }

    object CatsXml {
      lazy val dedicated: Seq[ModuleID] = List(
        "com.github.geirolz" %% "cats-xml" % "0.0.20"
      )
    }
  }

  object Plugins {
    val compilerPlugins: Seq[ModuleID] = Nil
  }

  object Docs {
    lazy val dedicated: Seq[ModuleID] = Nil
  }
}
