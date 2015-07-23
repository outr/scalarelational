package org.scalarelational.mapper

import scala.language.experimental.macros

import org.scalarelational.table.Table
import org.scalarelational.column.ColumnValue
import org.scalarelational.instruction.{Update, InsertSingle}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Entity {
  def mapTo[T <: Entity](table: Table): List[ColumnValue[Any]] =
    macro mapped.mapTo[T]

  def columns: List[ColumnValue[Any]]

  def insert: InsertSingle = {
    val values = columns
    val table = values.head.column.table
    insertColumnValues(table, values)
  }

  def update: Update = {
    val values = columns
    val table = values.head.column.table
    updateColumnValues(table, values)
  }
}