import sbt._
import Keys._

object OUTRQueryBuild extends Build {
  val baseSettings = Defaults.defaultSettings ++ Seq(
    version := "1.0.0-SNAPSHOT",
    organization := "com.outr.query",
    scalaVersion := "2.10.3",
    libraryDependencies ++= Seq(
      Dependencies.PowerScalaProperty,
      Dependencies.H2,
      Dependencies.Specs2
    ),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false
  )

  private def createSettings(_name: String) = baseSettings ++ Seq(name := _name)

  // Aggregator
  lazy val root = Project("root", file("."), settings = createSettings("outrquery"))
    .aggregate(core, orm, h2)

  lazy val core = Project("core", file("core"), settings = createSettings("outrquery-core"))
  lazy val orm = Project("orm", file("orm"), settings = createSettings("outrquery-orm"))
    .dependsOn(core)

  // Implementations
  lazy val h2 = Project("h2", file("h2"), settings = createSettings("outrquery-h2"))
    .dependsOn(core)
}

object Dependencies {
  private val PowerScalaVersion = "1.6.3-SNAPSHOT"

  val PowerScalaProperty = "org.powerscala" %% "powerscala-property" % PowerScalaVersion
  val H2 = "com.h2database" % "h2" % "1.3.174"
  val Specs2 = "org.specs2" %% "specs2" % "2.2.3" % "test"
}
