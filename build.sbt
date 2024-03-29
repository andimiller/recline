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
        "com.monovore"   %% "decline"  % "1.0.0",
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
          "io.circe"      %% "circe-generic" % "0.12.0",
          "io.circe"      %% "circe-parser"  % "0.12.0",
          "io.circe"      %% "circe-yaml"    % "0.12.0",
          "org.typelevel" %% "cats-effect"   % "2.0.0"
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
      excludeFilter in ghpagesCleanSite :=
        new FileFilter {
          def accept(f: File) = (ghpagesRepository.value / "CNAME").getCanonicalPath == f.getCanonicalPath
        },
      micrositeName := "recline",
      micrositeDescription := "an opinionated configuration framework",
      micrositeAuthor := "Andi Miller",
      micrositeOrganizationHomepage := "http://andimiller.net",
      micrositeGithubOwner := "andimiller",
      micrositeGithubRepo := "recline",
      micrositeCompilingDocsTool := WithMdoc,
      mdocIn := sourceDirectory.value / "main" / "docs",
      micrositeDocumentationUrl := "/getting-started.html",
      scalacOptions.in(Compile) ~= filterConsoleScalacOptions,
      mdocVariables := Map(
        "VERSION" -> version.value
      ),
      micrositePalette := Map(
        "brand-primary"   -> "#FFB6AD",
        "brand-secondary" -> "#805B57",
        "brand-tertiary"  -> "#850E00",
        "gray-dark"       -> "#453E46",
        "gray"            -> "#837F84",
        "gray-light"      -> "#E3E2E3",
        "gray-lighter"    -> "#F4F3F4",
        "white-color"     -> "#FFFFFF"
      )
    )
  )
