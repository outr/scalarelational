package org.scalarelational.instruction.ddl

import org.scalarelational.datatype.DataType
import org.scalarelational.column.ColumnPropertyContainer
import org.scalarelational.column.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CreateColumn[T](tableName: String, name: String, dataType: DataType[T], props: Seq[ColumnProperty])
                          (implicit manifest: Manifest[T]) extends ColumnPropertyContainer {
  override def classType = manifest.runtimeClass

  this.props(props: _*)
}