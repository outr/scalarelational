package org.scalarelational.instruction.ddl

import org.scalarelational.column.ColumnPropertyContainer
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType

case class CreateColumn[T, S](tableName: String,
                              name: String,
                              dataType: DataType[T, S],
                              props: Seq[ColumnProperty]
                             ) extends ColumnPropertyContainer {
  this.props(props: _*)

  override def optional: Boolean = dataType.optional
}