package org.scalarelational.mapper

import org.scalarelational.column.{Column, ColumnValue}
import org.scalarelational.datatype.{Id, Ref}
import org.scalarelational.instruction.{Delete, InsertSingle, Update}

import scala.language.experimental.macros

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait BaseEntity[Mapped] {
  def insert: InsertSingle[Ref[Mapped]]
  def update: Update[Ref[Mapped]]
}

trait Entity[Mapped] extends Id[Mapped] {
  def mapTo[T <: Entity[T]](table: MappedTable[_]): List[ColumnValue[Any, Any]] =
    macro Mapped.mapTo[T]

  def columns: List[ColumnValue[Any, Any]]

  def delete: Delete = if (id.nonEmpty) {
    val table = columns.head.column.table
    val primaryKey = table.primaryKey.asInstanceOf[Column[Option[Int], Int]]
    table.datastore.delete(table) where primaryKey === id
  } else {
    throw new RuntimeException("Cannot delete un-persisted entity.")
  }

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