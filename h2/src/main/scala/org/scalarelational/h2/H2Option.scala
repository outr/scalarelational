package org.scalarelational.h2

/**
  * See http://www.h2database.com/javadoc/org/h2/constant/DbSettings.html for most of the options available.
  */
class H2Option(val k: String, val v: String) {
  def this(k: String, b: Boolean) = this(k, b.toString.toUpperCase)
  def this(k: String, i: Int) = this(k, i.toString)
  def this(k: String, l: Long) = this(k, l.toString)

  lazy val value = s"$k=$v"
}

object AliasColumnName extends H2Option("ALIAS_COLUMN_NAME", true)
case class AnalyzeAuto(changes: Int = 0) extends H2Option("ANALYZE_AUTO", changes)
case class AnalyzeSample(sample: Int = AnalyzeSample.Default) extends H2Option("ANALYZE_SAMPLE", sample)
object AnalyzeSample {
  val Default: Int = 10000
}
object DatabaseToUpper extends H2Option("DATABASE_TO_UPPER", true)
object DatabaseToLower extends H2Option("DATABASE_TO_UPPER", false)
case class DbCloseOnExit(close: Boolean = true) extends H2Option("DB_CLOSE_ON_EXIT", close)
object AutoServer extends H2Option("AUTO_SERVER", true)
case class AutoServerPort(port: Int) extends H2Option("AUTO_SERVER_PORT", port)
case class CacheSize(sizeInKb: Long) extends H2Option("CACHE_SIZE", sizeInKb)
case class CacheType(cacheType: String, soft: Boolean = false) extends H2Option("CACHE_TYPE", s"${if (soft) "SOFT_" else ""}$cacheType")
case class CloseDelay(delay: Long) extends H2Option("DB_CLOSE_DELAY", delay)
object MVStore extends H2Option("MV_STORE", true)
object MultiThreaded extends H2Option("MULTI_THREADED", true)