package org.scalarelational.mapper

import org.scalarelational.ColumnValue
import org.scalarelational.instruction.{Update, InsertSingle}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableMappable {
  def toColumnValues: List[ColumnValue[Any]] =
    throw new RuntimeException(s"@mapped annotation missing for table ${this.getClass}")

  def insert: InsertSingle = {
    val values = toColumnValues
    val table = values.head.column.table
    insertColumnValues(table, values)
  }

  def update: Update = {
    val values = toColumnValues
    val table = values.head.column.table
    updateColumnValues(table, values)
  }
}