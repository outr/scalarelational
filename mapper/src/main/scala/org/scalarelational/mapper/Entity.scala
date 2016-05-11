package org.scalarelational.mapper

import org.scalarelational.column.property.PrimaryKey
import org.scalarelational.column.{Column, ColumnLike, ColumnValue}
import org.scalarelational.compiletime.Macros
import org.scalarelational.datatype.{Id, Ref}
import org.scalarelational.instruction.{Delete, InsertSingle, Update}

import scala.language.experimental.macros


trait BaseEntity[Mapped] {
  def insert: InsertSingle[Ref[Mapped]]
  def update: Update[Ref[Mapped]]
}

trait Entity[Mapped] extends Id[Mapped] {
  def mapTo[T <: Entity[T]](table: MappedTable[_]): List[ColumnValue[Any, Any]] =
    macro Macros.mapTo[T]

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

  /**
    * Provides support to limit the columns being updated to the ones referenced in the argument list.
    */
  def update(head: ColumnLike[_, _], tail: ColumnLike[_, _]*): Update[Ref[Mapped]] = {
    val values = columns
    head.table match {
      case mt: MappedTable[Mapped] => {
        var included: Set[ColumnLike[Any, Any]] = Set(head.asInstanceOf[ColumnLike[Any, Any]]) ++ tail.asInstanceOf[Seq[ColumnLike[Any, Any]]]
        if (!included.exists(column => column.has(PrimaryKey))) {
          included += mt.primaryKey.asInstanceOf[Column[Any, Any]]
        }
        val filtered = values.filter(cv => included.contains(cv.column))
        mt.updateColumnValues(filtered)
      }
      case _ => throw new RuntimeException("Entity can only be used with MappedTables")
    }
  }
}