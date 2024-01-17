import sbt.*
import scala.language.postfixOps

object ProjectDependencies {

  // base
  private val catsVersion = "2.10.0"
  // test
  private val munitVersion       = "0.7.29"
  private val munitEffectVersion = "1.0.7"
  private val scalacheck         = "1.17.0"
  // integrations
  private val catsEffectVersion     = "3.5.3"
  private val pureConfigVersion     = "0.17.4"
  private val typesafeConfigVersion = "1.4.3"
  private val cirisVersion          = "3.5.0"

  lazy val common: Seq[ModuleID] = Seq(
    // base
    "org.typelevel" %% "cats-core" % catsVersion % Provided,

    // test
    "org.scalameta"  %% "munit"            % munitVersion % Test,
    "org.scalameta"  %% "munit-scalacheck" % munitVersion % Test,
    "org.scalacheck" %% "scalacheck"       % scalacheck   % Test
  )

  object Core {
    lazy val dedicated: Seq[ModuleID] = Nil
  }

  object Integrations {

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
  }

  object Plugins {
    val compilerPlugins: Seq[ModuleID] = Nil
  }

  object Docs {
    lazy val dedicated: Seq[ModuleID] = Nil
  }
}
