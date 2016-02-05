import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys._

object ScalaRelationalBuild extends Build {
  import Dependencies._

  lazy val root = Project(
    id = "root",
    base = file(".")
  ).settings(name := "ScalaRelational", publish := {})
   .aggregate(dsl, model, h2)
  lazy val dsl = project("dsl").withDependencies(enumeratum, shapeless, logging, scalaTest)
  lazy val model = project("model").dependsOn(dsl).withDependencies(enumeratum, logging, hikariCP, scalaTest, metaRx).settings(
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
  )
  lazy val h2 = project("h2").withDependencies(h2database, scalaTest).dependsOn(model, model % "test->test")

  private def project(projectName: String) = Project(id = projectName, base = file(projectName)).settings(
    name := s"${Details.name}-$projectName",
    version := Details.version,
    organization := Details.organization,
    scalaVersion := Details.scalaVersion,
    sbtVersion := Details.sbtVersion,
    fork := true,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      Resolver.typesafeRepo("releases")
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT")) {
          Some("snapshots" at nexus + "content/repositories/snapshots")
        } else {
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
        }
    },
    coverageEnabled := true,
    publishArtifact in Test := false,
    pomExtra := <url>${Details.url}</url>
      <licenses>
        <license>
          <name>{Details.licenseType}</name>
          <url>{Details.licenseURL}</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <developerConnection>scm:{Details.repoURL}</developerConnection>
        <connection>scm:{Details.repoURL}</connection>
        <url>{Details.projectURL}</url>
      </scm>
      <developers>
        <developer>
          <id>{Details.developerId}</id>
          <name>{Details.developerName}</name>
          <url>{Details.developerURL}</url>
        </developer>
      </developers>
  )

  implicit class EnhancedProject(project: Project) {
    def withDependencies(modules: ModuleID*): Project = project.settings(libraryDependencies ++= modules)
  }
}

object Details {
  val organization = "org.scalarelational"
  val name = "scalarelational"
  val version = "2.0.0-SNAPSHOT"
  val url = "http://outr.com"
  val licenseType = "Apache 2.0"
  val licenseURL = "http://opensource.org/licenses/Apache-2.0"
  val projectURL = "https://github.com/darkfrog26/scalarelational"
  val repoURL = "https://github.com/darkfrog26/scalarelational.git"
  val developerId = "darkfrog"
  val developerName = "Matt Hicks"
  val developerURL = "http://matthicks.com"

  val sbtVersion = "0.13.9"
  val scalaVersion = "2.11.7"
}

object Dependencies {
  val hikariCP = "com.zaxxer" % "HikariCP" % "2.4.3"
  val h2database = "com.h2database" % "h2" % "1.4.191"
  val metaRx = "pl.metastack" %%  "metarx" % "0.1.4"
  val enumeratum = "com.beachape" %% "enumeratum" % "1.3.6"
  val shapeless = "com.chuusai" %% "shapeless" % "2.2.5"
  val logging = "com.outr.scribe" %% "scribe-core" % "1.0.0"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6" % "test"
}
