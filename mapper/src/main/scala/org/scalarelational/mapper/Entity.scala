package org.scalarelational.mapper

import scala.language.experimental.macros

import org.scalarelational.column.ColumnValue
import org.scalarelational.datatype.{Ref, Id}
import org.scalarelational.instruction.{InsertSingle, Update}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait BaseEntity[Mapped] {
  def insert: InsertSingle[Ref[Mapped]]
  def update: Update[Ref[Mapped]]
}

trait Entity[Mapped] extends Id[Mapped] {
  // TODO We cannot use MappedTable[T] because it would need to be co-variant
  def mapTo[T <: Entity[T]](table: MappedTable[_]): List[ColumnValue[Any]] =
    macro Mapped.mapTo[T]

  def columns: List[ColumnValue[Any]]

  def insert: InsertSingle[Ref[Mapped]] = {
    val values = columns
    values.head.column.table match {
      case mt: MappedTable[Mapped] => mt.insertColumnValues(values)
      case _ => throw new RuntimeException("Entity can only be used with MappedTables")
    }
  }

  def update: Update[Ref[Mapped]] = {
    val values = columns
    values.head.column.table match {
      case mt: MappedTable[Mapped] => mt.updateColumnValues(values)
      case _ => throw new RuntimeException("Entity can only be used with MappedTables")
    }
  }
}