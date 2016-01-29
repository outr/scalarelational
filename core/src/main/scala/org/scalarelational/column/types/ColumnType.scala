package org.scalarelational.column.types

trait ColumnType[T] {
  def optional: ColumnType[Option[T]] = OptionalType[T](this)
}
