package org.scalarelational.model

import org.scalarelational.datatype.{DataTypeGenerators, DataType}

case class ColumnOption[T](column: ColumnLike[T]) extends ColumnLike[Option[T]] {
  def name: String = column.name
  def longName: String = column.longName
  def table: Table = column.table
  def converter: DataType[Option[T]] = DataTypeGenerators.option[T](column.converter)
  def manifest: Manifest[Option[T]] = column.manifest.asInstanceOf[Manifest[Option[T]]]
}
