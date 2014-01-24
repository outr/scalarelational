package com.outr.query.property

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Indexed private(val indexName: String) extends ColumnProperty {
  def name = Indexed.name
}

object Indexed {
  val name = "indexed"

  def apply(indexName: String) = new Indexed(indexName)
}