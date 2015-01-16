import Dependencies._
import sbt.Keys._
import sbt._

object OUTRQueryBuild extends Build {
  val baseSettings = Defaults.coreDefaultSettings ++ Seq(
    version := "1.0.3",
    organization := "com.outr.query",
    scalaVersion := "2.11.4",
    libraryDependencies ++= Seq(
      PowerScalaProperty,
      Unbescape,
      H2,
      ScalaTest
    ),
    fork := true,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    testOptions in Test += Tests.Argument("-oDF"),
    pomExtra := <url>http://outr.com</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://www.opensource.org/licenses/bsd-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <developerConnection>scm:https://github.com/darkfrog26/outrquery.git</developerConnection>
        <connection>scm:https://github.com/darkfrog26/outrquery.git</connection>
        <url>https://github.com/darkfrog26/outrquery</url>
      </scm>
      <developers>
        <developer>
          <id>darkfrog</id>
          <name>Matt Hicks</name>
          <url>http://matthicks.com</url>
        </developer>
      </developers>
  )

  private def createSettings(_name: String) = baseSettings ++ Seq(name := _name)

  // Aggregator
  lazy val root = Project("root", file("."), settings = createSettings("outrquery"))
    .aggregate(core, h2, orm, search)

  lazy val core = Project("core", file("core"), settings = createSettings("outrquery-core"))

  // Implementations
  lazy val h2 = Project("h2", file("h2"), settings = createSettings("outrquery-h2"))
    .dependsOn(core)

  // Add-ons
  lazy val orm = Project("orm", file("orm"), settings = createSettings("outrquery-orm"))
    .dependsOn(core, h2)
  lazy val search = Project("search", file("search"), settings = createSettings("outrquery-search"))
    .dependsOn(core, h2, orm)
    .settings(libraryDependencies ++= Seq(PowerScalaSearch))
}

object Dependencies {
  private val PowerScalaVersion = "1.6.7"

  val PowerScalaProperty = "org.powerscala" %% "powerscala-property" % PowerScalaVersion
  val PowerScalaSearch = "org.powerscala" %% "powerscala-search" % PowerScalaVersion
  val H2 = "com.h2database" % "h2" % "1.4.182"
  val Unbescape = "org.unbescape" % "unbescape" % "1.1.0.RELEASE"
  val ScalaTest = "org.scalatest" %% "scalatest" % "2.2.2" % "test"
}
