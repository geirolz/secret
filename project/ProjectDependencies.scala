import sbt.*
import scala.language.postfixOps

object ProjectDependencies {

  // base
  private val catsVersion   = "2.13.0"
  private val bcryptVersion = "0.10.2"
  // test
  private val munitVersion       = "1.1.0"
  private val munitEffectVersion = "2.0.0"
  private val scalacheck         = "1.18.1"
  // integrations
  private val catsEffectVersion     = "3.5.7"
  private val pureConfigVersion     = "0.17.8"
  private val typesafeConfigVersion = "1.4.3"
  private val cirisVersion          = "3.7.0"

  lazy val common: Seq[ModuleID] = Seq(
    // base
    "org.typelevel" %% "cats-core" % catsVersion,
    "at.favre.lib"   % "bcrypt"    % bcryptVersion,

    // test
    "org.typelevel"  %% "cats-effect"       % catsEffectVersion  % Test,
    "org.typelevel"  %% "munit-cats-effect" % munitEffectVersion % Test,
    "org.scalameta"  %% "munit"             % munitVersion       % Test,
    "org.scalameta"  %% "munit-scalacheck"  % munitVersion       % Test,
    "org.scalacheck" %% "scalacheck"        % scalacheck         % Test
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
        "org.typelevel" %% "cats-effect"       % catsEffectVersion,
        "org.typelevel" %% "munit-cats-effect" % munitEffectVersion % Test
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
        "org.typelevel" %% "cats-effect"       % catsEffectVersion  % Test,
        "org.typelevel" %% "munit-cats-effect" % munitEffectVersion % Test
      )
    }

    object Circe {
      lazy val dedicated: Seq[ModuleID] = List(
        "io.circe" %% "circe-core" % "0.14.10"
      )
    }

    object CatsXml {
      lazy val dedicated: Seq[ModuleID] = List(
        "com.github.geirolz" %% "cats-xml" % "0.0.19"
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
