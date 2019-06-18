import xerial.sbt.Sonatype._
import ReleaseTransformations._

skip in publish := true

lazy val sharedSettings = Seq(
  organization := "net.andimiller",
  scalacOptions += "-Ypartial-unification",
  scalafmtConfig in ThisBuild := Some(file("scalafmt.conf")),
  scalaVersion := "2.12.8",
  // release settings
  useGpg := true,
  publishTo := sonatypePublishTo.value,
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  sonatypeProjectHosting := Some(GitHubHosting("andimiller", "whales", "andi at andimiller dot net")),
  developers := List(Developer(id = "andimiller", name = "Andi Miller", email = "andi@andimiller.net", url = url("http://andimiller.net"))),
  releaseCrossBuild := false,
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
)

lazy val macros = (project in file("macros"))
  .settings(
    sharedSettings ++ List(
      name := "recline-macros",
      libraryDependencies ++= List(
        "com.monovore"   %% "decline"  % "0.6.2",
        "com.propensive" %% "magnolia" % "0.10.0",
      )
    )
  )
lazy val core = (project in file("core"))
  .dependsOn(macros)
  .settings(
    sharedSettings ++
      List(
        name := "recline",
        libraryDependencies ++= List(
          "io.circe" %% "circe-generic" % "0.11.1",
          "io.circe" %% "circe-yaml"    % "0.8.0"
        )
      ))
lazy val test = (project in file("test"))
  .dependsOn(core)
  .settings(
    sharedSettings ++ List(
      name := "recline-test",
      skip in publish := true,
      libraryDependencies ++= List(
        "org.scalatest" %% "scalatest" % "3.0.8" % "test"
      )
    ))
