import sbt.project

lazy val prjName                = "secret"
lazy val prjDescription         = "A functional, type-safe and memory-safe class to handle secret values"
lazy val org                    = "com.github.geirolz"
lazy val scala33                = "3.3.1"
lazy val supportedScalaVersions = List(scala33)

//## global project to no publish ##
val copyReadMe = taskKey[Unit]("Copy generated README to main folder.")
lazy val root: Project = project
  .in(file("."))
  .settings(
    inThisBuild(
      List(
        homepage := Some(url(s"https://github.com/geirolz/$prjName")),
        licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
        developers := List(
          Developer(
            "DavidGeirola",
            "David Geirola",
            "david.geirola@gmail.com",
            url("https://github.com/geirolz")
          )
        )
      )
    )
  )
  .settings(baseSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Nil
  )
  .settings(
    copyReadMe := IO.copyFile(file("docs/compiled/README.md"), file("README.md"))
  )
  .aggregate(core, docs, pureconfig, typesafeConfig)

lazy val docs: Project =
  project
    .in(file("docs"))
    .enablePlugins(MdocPlugin)
    .dependsOn(core, pureconfig, typesafeConfig)
    .settings(
      baseSettings,
      noPublishSettings,
      libraryDependencies ++= ProjectDependencies.Docs.dedicated,
      // config
      scalacOptions --= Seq("-Werror", "-Xfatal-warnings"),
      mdocIn  := file("docs/source"),
      mdocOut := file("docs/compiled"),
      mdocVariables := Map(
        "VERSION"  -> previousStableVersion.value.getOrElse("<version>"),
        "DOC_OUT"  -> mdocOut.value.getPath,
        "PRJ_NAME" -> prjName,
        "ORG"      -> org
      )
    )

lazy val core: Project =
  module("core")(
    folder    = "./core",
    publishAs = Some(prjName)
  ).settings(
    libraryDependencies ++= ProjectDependencies.Core.dedicated
  )

// integrations
lazy val integrationsFolder: String = "./integrations"
lazy val pureconfig: Project =
  module("pureconfig")(
    folder    = s"$integrationsFolder/pureconfig",
    publishAs = Some(subProjectName("pureconfig"))
  ).dependsOn(core)
    .settings(
      libraryDependencies ++= ProjectDependencies.Integrations.Pureconfig.dedicated
    )

lazy val typesafeConfig: Project =
  module("typesafe-config")(
    folder    = s"$integrationsFolder/typesafe-config",
    publishAs = Some(subProjectName("typesafe-config"))
  ).dependsOn(core)
    .settings(
      libraryDependencies ++= ProjectDependencies.Integrations.TypesafeConfig.dedicated
    )

//=============================== MODULES UTILS ===============================
def module(modName: String)(
  folder: String,
  publishAs: Option[String]       = None,
  mimaCompatibleWith: Set[String] = Set.empty
): Project = {
  val keys       = modName.split("-")
  val modDocName = keys.mkString(" ")
  val publishSettings = publishAs match {
    case Some(pubName) =>
      Seq(
        moduleName     := pubName,
        publish / skip := false
      )
    case None => noPublishSettings
  }
  val mimaSettings = Seq(
    mimaFailOnNoPrevious := false,
    mimaPreviousArtifacts := mimaCompatibleWith.map { version =>
      organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % version
    }
  )

  Project(modName, file(folder))
    .settings(
      name := s"$prjName $modDocName",
      mimaSettings,
      publishSettings,
      baseSettings
    )
}

def subProjectName(modPublishName: String): String = s"$prjName-$modPublishName"

//=============================== SETTINGS ===============================
lazy val noPublishSettings: Seq[Def.Setting[_]] = Seq(
  publish              := {},
  publishLocal         := {},
  publishArtifact      := false,
  publish / skip       := true,
  mimaFailOnNoPrevious := false
)

lazy val baseSettings: Seq[Def.Setting[_]] = Seq(
  // project
  name         := prjName,
  description  := prjDescription,
  organization := org,
  // scala
  crossScalaVersions := supportedScalaVersions,
  scalaVersion       := supportedScalaVersions.head,
  scalacOptions ++= scalacSettings(scalaVersion.value),
  versionScheme := Some("early-semver"),
  // dependencies
  resolvers ++= ProjectResolvers.all,
  libraryDependencies ++= ProjectDependencies.common ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => ProjectDependencies.Plugins.compilerPlugins
      case _            => Nil
    }
  }
)

def scalacSettings(scalaVersion: String): Seq[String] =
  Seq(
    //    "-Xlog-implicits",
    //    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8", // Specify character encoding used by source files.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    //    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-language:dynamics"
  ) ++ {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((3, _)) =>
        Seq(
          "-Ykind-projector",
          "-explain-types", // Explain type errors in more detail.
          "-Xfatal-warnings" // Fail the compilation if there are any warnings.
        )
      case _ => Nil
    }
  }

//=============================== ALIASES ===============================
addCommandAlias("check", "scalafmtAll;clean;coverage;test;coverageAggregate")
addCommandAlias("gen-doc", "mdoc;copyReadMe;")
addCommandAlias("coverage-test", "coverage;test;coverageReport")
