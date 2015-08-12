package org.scalarelational.instruction.ddl

import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ChangeColumnType[T, S](tableName: String, columnName: String, dataType: DataType[T, S], properties: ColumnProperty*)
                              (implicit val manifest: Manifest[T])