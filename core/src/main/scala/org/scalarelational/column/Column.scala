package org.scalarelational.column

import org.scalarelational.column.types.ColumnType

case class Column[T](name: String, columnType: ColumnType[T])