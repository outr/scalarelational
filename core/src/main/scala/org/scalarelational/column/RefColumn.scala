package org.scalarelational.column

import org.scalarelational.datatype.{DataType, Ref}
import org.scalarelational.table.Table

import scala.language.existentials

case class RefColumn[T](column: ColumnLike[T, Int]) extends ColumnLike[Ref[T], Int] {
  def name: String = column.name
  def longName: String = column.longName
  def table: Table = column.table
  def dataType: DataType[Ref[T], Int] = column.dataType.asInstanceOf[DataType[Ref[T], Int]]
  def manifest: Manifest[Ref[T]] = column.manifest.asInstanceOf[Manifest[Ref[T]]]

  override def classType = manifest.runtimeClass
  override def properties = column.properties
}
