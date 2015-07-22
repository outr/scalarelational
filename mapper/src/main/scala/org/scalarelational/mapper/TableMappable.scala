package org.scalarelational.mapper

import org.scalarelational.ColumnValue
import org.scalarelational.instruction.{Update, InsertSingle, Instruction}
import org.scalarelational.model.property.column.property.PrimaryKey

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableMappable {
  def toColumnValues: List[ColumnValue[Any]] =
    throw new RuntimeException(s"@mapped annotation missing for table ${this.getClass}")

  def persist(): Instruction[Int] = {
    val values = toColumnValues
    val table = values.head.column.table
    persistColumnValues(table, values)
  }

  def insert(): InsertSingle = {
    val values = toColumnValues
    val table = values.head.column.table
    insertColumnValues(table, values)
  }

  def update(): Update = {
    val values = toColumnValues
    val table = values.head.column.table
    val primaryKey = values.find(cv => cv.column.has(PrimaryKey))
      .getOrElse(throw new RuntimeException("Update must have a PrimaryKey value specified to be able to update."))
    updateColumnValues(table, primaryKey, values)
  }
}