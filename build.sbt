organization := "net.andimiller"

name := "recline"

version := "0.0.1"

scalaVersion := "2.12.8"

scalacOptions += "-Ypartial-unification"

scalafmtConfig in ThisBuild := Some(file("scalafmt.conf"))

lazy val macros = (project in file("macros")).settings(
  name := "recline-macros",
  libraryDependencies ++= List(
    "com.monovore"   %% "decline"  % "0.6.2",
    "com.propensive" %% "magnolia" % "0.10.0",
  )
)
lazy val core = (project in file("core"))
  .dependsOn(macros)
  .settings(
    name := "recline",
    libraryDependencies ++= List(
      "io.circe" %% "circe-generic" % "0.11.1",
      "io.circe" %% "circe-yaml"    % "0.8.0"
    )
  )
lazy val test = (project in file("test"))
  .dependsOn(core)
  .settings(
    name := "recline-test",
    skip in publish := true,
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest" % "3.0.8" % "test"
    )
  )

// publishing/releasing settings
import xerial.sbt.Sonatype._

useGpg := true
publishTo := sonatypePublishTo.value
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
sonatypeProjectHosting := Some(GitHubHosting("andimiller", "whales", "andi at andimiller dot net"))
developers := List(Developer(id = "andimiller", name = "Andi Miller", email = "andi@andimiller.net", url = url("http://andimiller.net")))

import ReleaseTransformations._
releaseCrossBuild := false
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("+test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
