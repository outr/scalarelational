package org.scalarelational.instruction.ddl

import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType

case class ChangeColumnType[T, S](tableName: String, columnName: String, dataType: DataType[T, S], properties: ColumnProperty*) {
  def optional: Boolean = dataType.optional
}
