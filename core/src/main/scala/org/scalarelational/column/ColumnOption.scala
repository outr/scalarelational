package org.scalarelational.column

import org.scalarelational.datatype.DataType
import org.scalarelational.datatype.create.OptionDataTypeCreator
import org.scalarelational.table.Table

case class ColumnOption[T, S](column: ColumnLike[T, S]) extends ColumnLike[Option[T], S] {
  def name: String = column.name
  def longName: String = column.longName
  def table: Table = column.table
  def dataType: DataType[Option[T], S] = OptionDataTypeCreator.create[T, S](column.dataType)
  def manifest: Manifest[Option[T]] = column.manifest.asInstanceOf[Manifest[Option[T]]]

  override def classType = manifest.runtimeClass
  override def properties = column.properties
}
