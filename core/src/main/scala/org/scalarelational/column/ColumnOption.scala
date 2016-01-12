package org.scalarelational.column

import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType
import org.scalarelational.datatype.create.OptionDataTypeCreator
import org.scalarelational.table.Table

case class ColumnOption[T, S](column: ColumnLike[T, S]) extends ColumnLike[Option[T], S] {
  def name: String = column.name
  def longName: String = column.longName
  def table: Table = column.table
  def dataType: DataType[Option[T], S] = OptionDataTypeCreator.create[T, S](column.dataType)

  override def isOptional: Boolean = true

  override def properties: Map[String, ColumnProperty] = column.properties
}
