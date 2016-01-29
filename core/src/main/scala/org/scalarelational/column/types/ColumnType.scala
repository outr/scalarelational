package org.scalarelational.column.types

trait ColumnType[T] {
  def columnName: Option[String]
  def optional: ColumnType[Option[T]] = OptionalType[T](this)
}
