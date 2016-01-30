package org.scalarelational.column

import org.scalarelational.column.types.ColumnType
import org.scalarelational.table.Table

case class Column[T](columnType: ColumnType[T])(implicit table: Table) {
  def name: String = table.columnName(this)
}