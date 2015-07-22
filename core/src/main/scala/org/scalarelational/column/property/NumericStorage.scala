package org.scalarelational.column.property

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class NumericStorage(precision: Int, scale: Int) extends ColumnProperty {
  override def name = NumericStorage.Name
}

object NumericStorage {
  val Name = "numericStorage"
  val DefaultBigDecimal = NumericStorage(20, 2)
}