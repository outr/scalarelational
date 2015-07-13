package org.scalarelational.instruction.ddl

import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType
import org.scalarelational.model.ColumnPropertyContainer

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CreateColumn[T](tableName: String, name: String, dataType: DataType[T], properties: Map[String, ColumnProperty] = Map.empty)
                          (implicit manifest: Manifest[T]) extends ColumnPropertyContainer {
  override def classType = manifest.runtimeClass
}