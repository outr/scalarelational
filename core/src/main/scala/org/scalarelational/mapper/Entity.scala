package org.scalarelational.mapper

import scala.language.experimental.macros

import org.scalarelational.column.ColumnValue
import org.scalarelational.datatype.Id
import org.scalarelational.instruction.{InsertSingle, Update}
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Entity[Mapped] extends Id[Mapped] {
  def mapTo[T <: Entity[T]](table: Table[T]): List[ColumnValue[Any]] =
    macro mapped.mapTo[T]

  def columns: List[ColumnValue[Any]]

  def insert: InsertSingle[Mapped] = {
    val values = columns
    val table = values.head.column.table.asInstanceOf[Table[Mapped]]
    insertColumnValues(table, values)
  }

  def update: Update[Mapped] = {
    val values = columns
    val table = values.head.column.table.asInstanceOf[Table[Mapped]]
    updateColumnValues(table, values)
  }
}