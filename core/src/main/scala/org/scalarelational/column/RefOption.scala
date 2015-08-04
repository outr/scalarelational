package org.scalarelational.column

import scala.language.existentials

import org.scalarelational.table.Table
import org.scalarelational.datatype.{Ref, DataType}

case class RefOption[T](column: ColumnLike[_]) extends ColumnLike[Ref[T]] {
  def name: String = column.name
  def longName: String = column.longName
  def table: Table = column.table
  def dataType: DataType[Ref[T]] = column.dataType.asInstanceOf[DataType[Ref[T]]]
  def manifest: Manifest[Ref[T]] = column.manifest.asInstanceOf[Manifest[Ref[T]]]

  override def classType = manifest.runtimeClass
  override def properties = column.properties
}
