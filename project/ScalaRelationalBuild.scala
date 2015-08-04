import sbt.Keys._
import sbt._

object ScalaRelationalBuild extends Build {
  import Dependencies._

  lazy val root = Project(id = "root", base = file(".")).settings(name := "ScalaRelational", publish := {}).aggregate(core, h2, mysql, mapper, versioning)
  lazy val core = project("core").withDependencies(powerscala.property, hikariCP, scalaTest).settings(
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
  )
  lazy val h2 = project("h2").withDependencies(h2database, scalaTest).dependsOn(core, core % "test->test")
  lazy val mysql = project("mysql").withDependencies(mysqldatabase).dependsOn(core, core % "test->test")
  lazy val mapper = project("mapper").withDependencies(scalaTest).dependsOn(core, h2 % "test->test")
  lazy val versioning = project("versioning").withDependencies(scalaTest).dependsOn(core, h2 % "test->test")

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
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomExtra := <url>${Details.url}</url>
      <licenses>
        <license>
          <name>${Details.licenseType}</name>
          <url>${Details.licenseURL}</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <developerConnection>scm:${Details.repoURL}</developerConnection>
        <connection>scm:${Details.repoURL}</connection>
        <url>${Details.projectURL}</url>
      </scm>
      <developers>
        <developer>
          <id>${Details.developerId}</id>
          <name>${Details.developerName}</name>
          <url>${Details.developerURL}</url>
        </developer>
      </developers>
  )

  implicit class EnhancedProject(project: Project) {
    def withDependencies(modules: ModuleID*) = project.settings(libraryDependencies ++= modules)
  }
}

object Details {
  val organization = "org.scalarelational"
  val name = "scalarelational"
  val version = "1.1.0-SNAPSHOT"
  val url = "http://outr.com"
  val licenseType = "MIT"
  val licenseURL = "http://opensource.org/licenses/MIT"
  val projectURL = "https://github.com/darkfrog26/scalarelational"
  val repoURL = "https://github.com/darkfrog26/scalarelational.git"
  val developerId = "darkfrog"
  val developerName = "Matt Hicks"
  val developerURL = "http://matthicks.com"

  val sbtVersion = "0.13.8"
  val scalaVersion = "2.11.7"
}

object Dependencies {
  private val powerscalaVersion = "1.6.10"

  object powerscala {
    val property = "org.powerscala" %% "powerscala-property" % powerscalaVersion
  }
  val hikariCP = "com.zaxxer" % "HikariCP" % "2.3.9"
  val h2database = "com.h2database" % "h2" % "1.4.187"
  val mysqldatabase = "mysql" % "mysql-connector-java" % "5.1.16"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.5" % "test"
}
