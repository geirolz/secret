import sbt.*
import scala.language.postfixOps

object ProjectDependencies {

  // base
  private val catsVersion   = "2.10.0"
  private val bcryptVersion = "0.10.2"
  // test
  private val munitVersion       = "0.7.29"
  private val munitEffectVersion = "1.0.7"
  private val scalacheck         = "1.18.0"
  // integrations
  private val catsEffectVersion     = "3.5.4"
  private val pureConfigVersion     = "0.17.6"
  private val typesafeConfigVersion = "1.4.3"
  private val cirisVersion          = "3.6.0"

  lazy val common: Seq[ModuleID] = Seq(
    // base
    "org.typelevel" %% "cats-core" % catsVersion,
    "at.favre.lib"   % "bcrypt"    % bcryptVersion,

    // test
    "org.typelevel"  %% "cats-effect"         % catsEffectVersion  % Test,
    "org.typelevel"  %% "munit-cats-effect-3" % munitEffectVersion % Test,
    "org.scalameta"  %% "munit"               % munitVersion       % Test,
    "org.scalameta"  %% "munit-scalacheck"    % munitVersion       % Test,
    "org.scalacheck" %% "scalacheck"          % scalacheck         % Test
  )

  object Core {
    lazy val dedicated: Seq[ModuleID] = Nil
  }

  object Integrations {

    object CatsEffect {
      lazy val dedicated: Seq[ModuleID] = List(
        "org.typelevel" %% "cats-effect"         % catsEffectVersion,
        "org.typelevel" %% "munit-cats-effect-3" % munitEffectVersion % Test
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
        "is.cir"        %% "ciris"               % cirisVersion,
        "org.typelevel" %% "cats-effect"         % catsEffectVersion  % Test,
        "org.typelevel" %% "munit-cats-effect-3" % munitEffectVersion % Test
      )
    }

    object Circe {
      lazy val dedicated: Seq[ModuleID] = List(
        "io.circe" %% "circe-core" % "0.14.7"
      )
    }

    object CatsXml {
      lazy val dedicated: Seq[ModuleID] = List(
        "com.github.geirolz" %% "cats-xml" % "0.0.15"
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
