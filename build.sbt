// Types are not imported directly to avoid collisions with sbt's classes.
import org.typelevel.scalacoptions

ThisBuild / organization := "io.github.biochimia"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.16"

// License Headers
ThisBuild / organizationName := "João Abecasis"
ThisBuild / startYear := Some(2025)
ThisBuild / licenses += ("Apache-2.0", new URI("https://www.apache.org/licenses/LICENSE-2.0.txt").toURL)

// Scalafix
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies += "org.typelevel" %% "typelevel-scalafix" % "0.5.0"

val CatsVersion              = "2.13.0"
val CatsEffectVersion        = "3.6.1"
val CatsEffectTestingVersion = "1.6.0"
val ScalaTestVersion         = "3.2.19"

lazy val commonSettings = Seq(
  headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
  scalacOptions ++= scalacoptions.ScalacOptions.defaultTokensForVersion(
    scalacoptions.ScalaVersion.unsafeFromString(scalaVersion.value)
  ),
  Test / scalacOptions += "-Wconf:msg=unused value of type org.scalatest.Assertion:s",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core"                % CatsVersion,
    "org.scalatest" %% "scalatest-funsuite"       % ScalaTestVersion % Test,
    "org.scalatest" %% "scalatest-shouldmatchers" % ScalaTestVersion % Test,
  ),
)

lazy val catsRetry = project
  .in(file("cats-retry"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    commonSettings
  )

lazy val catsEffectRetry = project
  .in(file("cats-effect-retry"))
  .dependsOn(catsRetry)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect"                   % CatsEffectVersion,
      "org.typelevel" %% "cats-effect-testing-scalatest" % CatsEffectTestingVersion % Test,
    ),
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(catsRetry)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    commonSettings
  )
