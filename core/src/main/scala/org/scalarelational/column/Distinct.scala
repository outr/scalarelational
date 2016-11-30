package org.scalarelational.column

import org.scalarelational.datatype.DataType
import org.scalarelational.table.Table

case class Distinct[T, S](column: ColumnLike[T, S]) extends ColumnLike[T, S] {
  override def name: String = column.name

  override def longName: String = column.longName

  override def table: Table = column.table

  override def dataType: DataType[T, S] = column.dataType

  override def toSQL: String = s"DISTINCT ${column.toSQL}"
}
