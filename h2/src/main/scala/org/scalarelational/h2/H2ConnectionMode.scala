package org.scalarelational.h2

import java.io.File

import org.powerscala.Unique

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait H2ConnectionMode {
  def path: String
  def options: Seq[H2Option]

  private def optionsList = options.map(o => o.k -> o).toMap.values.map(o => o.value).toList

  def url = (path :: optionsList).mkString(";")
}

object H2ConnectionMode {
  def apply(connectionURL: String) = new H2ConnectionMode {
    override def path = null

    override def options = Nil

    override def url = connectionURL
  }
}

case class H2Embedded(file: File, options: H2Option*) extends H2ConnectionMode {
  def path = s"jdbc:h2:file:${file.getAbsolutePath}"
}

case class H2Memory(name: String, options: H2Option*) extends H2ConnectionMode {
  def path = s"jdbc:h2:mem:$name"
}

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
case class AnalyzeSample(sample: Int = 10000) extends H2Option("ANALYZE_SAMPLE", sample)
object DatabaseToUpper extends H2Option("DATABASE_TO_UPPER", true)
object DatabaseToLower extends H2Option("DATABASE_TO_UPPER", false)
case class DbCloseOnExit(close: Boolean = true) extends H2Option("DB_CLOSE_ON_EXIT", close)
object AutoServer extends H2Option("AUTO_SERVER", true)
case class AutoServerPort(port: Int) extends H2Option("AUTO_SERVER_PORT", port)
case class CacheSize(sizeInKb: Long) extends H2Option("CACHE_SIZE", sizeInKb)
case class CacheType(cacheType: String, soft: Boolean = false) extends H2Option("CACHE_TYPE", s"${if (soft) "SOFT_" else ""}$cacheType")
case class CloseDelay(delay: Long) extends H2Option("DB_CLOSE_DELAY", delay)
object MVStore extends H2Option("MV_STORE", true)