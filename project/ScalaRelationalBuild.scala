import sbt.Keys._
import sbt._

object ScalaRelationalBuild extends Build {
  import Dependencies._

  lazy val root = Project(
    id = "root",
    base = file(".")
  ).settings(name := "ScalaRelational", publish := {})
   .aggregate(core, macros, h2, mariadb, postgresql, mapper, versioning)
  lazy val core = project("core").withDependencies(enumeratum, logging, hikariCP, scalaTest, metaRx).settings(
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
  )
  lazy val macros = project("macros").withDependencies(scalaTest).dependsOn(core)
  lazy val h2 = project("h2").withDependencies(h2database, scalaTest).dependsOn(core, core % "test->test")
  lazy val mariadb = project("mariadb").withDependencies(mariadbdatabase).dependsOn(core, core % "test->test")
  lazy val postgresql = project("postgresql").withDependencies(postgresqldatabase).dependsOn(core, core % "test->test")
    .configs(PGSslTest)
    .settings(inConfig(PGSslTest)(Defaults.testTasks): _*)
    .settings(
      testOptions in Test := Seq(Tests.Filter(pgRegFilter)),
      testOptions in PGSslTest := Seq(Tests.Filter(pgSslFilter))
     )
  lazy val PGSslTest = config("pgssl") extend Test
  def pgRegFilter(name: String): Boolean = (name endsWith "Spec") && !pgSslFilter(name)
  def pgSslFilter(name: String):Boolean = name endsWith "SslSpec"

  lazy val mapper = project("mapper").withDependencies(scalaTest).dependsOn(core, macros, h2 % "test->test")
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
        if (v.trim.endsWith("SNAPSHOT")) {
          Some("snapshots" at nexus + "content/repositories/snapshots")
        } else {
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
        }
    },
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
  val version = "1.3.0-SNAPSHOT"
  val url = "http://outr.com"
  val licenseType = "MIT"
  val licenseURL = "http://opensource.org/licenses/MIT"
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
  val mariadbdatabase = "mysql" % "mysql-connector-java" % "5.1.38"
  val postgresqldatabase = "org.postgresql" % "postgresql" % "9.4.1207"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  val metaRx = "pl.metastack" %%  "metarx" % "0.1.4"
  val enumeratum = "com.beachape" %% "enumeratum" % "1.3.6"
  val logging = "com.outr.scribe" %% "scribe-core" % "1.0.0"
}
