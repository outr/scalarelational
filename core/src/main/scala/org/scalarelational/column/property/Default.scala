package org.scalarelational.column.property

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Default(value: String) extends ColumnProperty {
  def name = Default.name
}

object Default {
  val name = "default"
}