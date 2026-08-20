import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"

onLoad in Global ~= (_ andThen ("git config core.hooksPath hooks" :: _))

lazy val microservice = Project("construction-industry-scheme", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    PlayKeys.playDefaultPort := 6994,
    scalaSettings,
    defaultSettings(),
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;" +
    ".*Routes.*;.*viewmodels.govuk.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 78,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  )
  .settings(CodeCoverageSettings.settings *)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)

// Opt-in e2e suite against locally running services (see e2e/README.md).
// Not aggregated by the root project: runs only via `sbt e2e/test`.
lazy val e2e = project
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    // tests live in e2e/test (Play-style layout, like it/test) - without the
    // PlayScala plugin this must be set explicitly
    Test / unmanagedSourceDirectories := Seq(baseDirectory.value / "test"),
    Test / unmanagedResourceDirectories := Seq(baseDirectory.value / "test" / "resources"),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    libraryDependencies ++= AppDependencies.e2e
  )
