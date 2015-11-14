package org.scalarelational.column.property


class Indexed private(val indexName: String) extends ColumnProperty {
  def name = Indexed.name
}

object Indexed {
  val name = "indexed"

  def apply(indexName: String) = new Indexed(indexName)
}