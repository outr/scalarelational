import sbt._
import Keys._

import Dependencies._

object OUTRQueryBuild extends Build {
  val baseSettings = Defaults.defaultSettings ++ Seq(
    version := "1.0.0-SNAPSHOT",
    organization := "com.outr.query",
    scalaVersion := "2.10.3",
    libraryDependencies ++= Seq(
      PowerScalaProperty,
      H2,
      Specs2
    ),
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
    testOptions in Test += Tests.Argument("sequential")
  )

  private def createSettings(_name: String) = baseSettings ++ Seq(name := _name)

  // Aggregator
  lazy val root = Project("root", file("."), settings = createSettings("outrquery"))
    .aggregate(core, orm, search, h2)

  lazy val core = Project("core", file("core"), settings = createSettings("outrquery-core"))

  // Implementations
  lazy val h2 = Project("h2", file("h2"), settings = createSettings("outrquery-h2"))
    .dependsOn(core)

  // Add-ons
  lazy val orm = Project("orm", file("orm"), settings = createSettings("outrquery-orm"))
    .dependsOn(core, h2)
  lazy val search = Project("search", file("search"), settings = createSettings("outrquery-search"))
    .dependsOn(core, h2)
    .settings(libraryDependencies ++= Seq(LuceneCore, LuceneAnalyzersCommon, LuceneQueries, LuceneQueryParser))
}

object Dependencies {
  private val PowerScalaVersion = "1.6.3-SNAPSHOT"
  private val LuceneVersion = "4.6.1"

  val PowerScalaProperty = "org.powerscala" %% "powerscala-property" % PowerScalaVersion
  val H2 = "com.h2database" % "h2" % "1.3.174"
  val LuceneCore = "org.apache.lucene" % "lucene-core" % LuceneVersion
  val LuceneAnalyzersCommon = "org.apache.lucene" % "lucene-analyzers-common" % LuceneVersion
  val LuceneQueries = "org.apache.lucene" % "lucene-queries" % LuceneVersion
  val LuceneQueryParser = "org.apache.lucene" % "lucene-queryparser" % LuceneVersion
  val Specs2 = "org.specs2" %% "specs2" % "2.2.3" % "test"
}
