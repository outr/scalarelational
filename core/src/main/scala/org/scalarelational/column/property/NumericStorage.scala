package org.scalarelational.column.property

case class NumericStorage(precision: Int, scale: Int) extends ColumnProperty {
  override def name: String = NumericStorage.Name
}

object NumericStorage {
  val Name = "numericStorage"
  val Precision = 20
  val Scale = 10
  val DefaultBigDecimal: NumericStorage = NumericStorage(Precision, Scale)
}