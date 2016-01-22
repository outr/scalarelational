package org.scalarelational.column

import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType
import org.scalarelational.table.Table

case class ColumnOverride[R, T, S](column: ColumnLike[T, S],
                                   dataTypeOverride: DataType[R, S]) extends ColumnLike[R, S] {
  def name: String = column.name
  def longName: String = column.longName
  def table: Table = column.table
  def dataType: DataType[R, S] = dataTypeOverride

  override def properties: Map[String, ColumnProperty] = column.properties
}