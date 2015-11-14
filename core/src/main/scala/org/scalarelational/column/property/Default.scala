package org.scalarelational.column.property


case class Default(value: String) extends ColumnProperty {
  def name = Default.name
}

object Default {
  val name = "default"
}