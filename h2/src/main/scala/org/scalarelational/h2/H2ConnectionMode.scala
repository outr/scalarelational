package org.scalarelational.h2

import java.io.File

import org.powerscala.Unique

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait H2ConnectionMode {
  def path: String
  def options: Seq[H2Option]

  def url = (path :: options.map(o => o.value).toList).mkString(";")
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

sealed trait H2Option {
  def value: String
}

case class CloseDelay(delay: Long) extends H2Option {
  def value = s"DB_CLOSE_DELAY=$delay"
}

object AutoServer extends H2Option {
  def value = "AUTO_SERVER=TRUE"
}

case class AutoServerPort(port: Int) extends H2Option {
  def value = s"AUTO_SERVER_PORT=$port"
}