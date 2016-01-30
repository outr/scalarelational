package org.scalarelational.column

import org.scalarelational.column.types.ColumnType
import org.scalarelational.table.Table

class Column[T](val columnType: ColumnType[T])(implicit val table: Table) {
  def name: String = table.columnName(this)
}