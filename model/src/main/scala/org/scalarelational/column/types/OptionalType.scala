package org.scalarelational.column.types

case class OptionalType[T](columnType: ColumnType[T]) extends ColumnType[Option[T]] {
  def columnName: Option[String] = columnType.columnName
}