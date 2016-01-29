package org.scalarelational.column.types

case class OptionalType[T](columnType: ColumnType[T]) extends ColumnType[Option[T]]