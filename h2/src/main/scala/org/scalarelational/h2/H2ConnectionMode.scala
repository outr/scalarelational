package org.scalarelational.h2

import java.io.File

trait H2ConnectionMode {
  def path: String
  def options: Seq[H2Option]

  private def optionsList = options.map(o => o.k -> o).toMap.values.map(o => o.value).toList

  def url: String = (path :: optionsList).mkString(";")
}

object H2ConnectionMode {
  def apply(connectionURL: String): H2ConnectionMode = new H2ConnectionMode {
    override def path: String = ""

    override def options: Seq[H2Option] = Nil

    override def url: String = connectionURL
  }
}

case class H2Embedded(file: File, options: H2Option*) extends H2ConnectionMode {
  def path: String = s"jdbc:h2:file:${file.getAbsolutePath}"
}

case class H2Memory(name: String, options: H2Option*) extends H2ConnectionMode {
  def path: String = s"jdbc:h2:mem:$name"
}