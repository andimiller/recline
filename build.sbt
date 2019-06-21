import xerial.sbt.Sonatype._
import ReleaseTransformations._

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

lazy val root = (project in file("."))
  .aggregate(macros, core, test)
  .settings(
    sharedSettings ++ List(
      skip in publish := true
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
          "io.circe"      %% "circe-generic" % "0.11.1",
          "io.circe"      %% "circe-parser"  % "0.11.1",
          "io.circe"      %% "circe-yaml"    % "0.8.0",
          "org.typelevel" %% "cats-effect"   % "1.3.1"
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

lazy val docs = (project in file("docs"))
  .dependsOn(core)
  .enablePlugins(MicrositesPlugin)
  .settings(
    sharedSettings ++ List(
      name := "recline-docs",
      skip in publish := true,
      // sbt-microsites settings
      micrositeName := "recline",
      micrositeDescription := "an opinionated configuration framework",
      micrositeAuthor := "Andi Miller",
      micrositeOrganizationHomepage := "http://andimiller.net",
      micrositeGithubOwner := "andimiller",
      micrositeGithubRepo := "recline",
      micrositeCompilingDocsTool := WithMdoc,
      mdocIn := sourceDirectory.value / "main" / "docs",
      scalacOptions.in(Compile) ~= filterConsoleScalacOptions,
      micrositePalette := Map(
        "brand-primary"   -> "#C9FDFF",
        "brand-secondary" -> "#657F80",
        "brand-tertiary"  -> "#97BEBF",
        "gray-dark"       -> "#453E46",
        "gray"            -> "#837F84",
        "gray-light"      -> "#E3E2E3",
        "gray-lighter"    -> "#F4F3F4",
        "white-color"     -> "#FFFFFF"
      )
    )
  )
