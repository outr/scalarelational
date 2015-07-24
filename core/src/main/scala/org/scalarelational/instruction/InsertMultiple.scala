package org.scalarelational.instruction

import org.scalarelational.table.Table
import org.scalarelational.column.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertMultiple[T](table: Table[T], rows: Seq[Seq[ColumnValue[_]]])
  extends Insert with Instruction[T, List[Int]] {
  def result = table.datastore.exec(this)

  def and(nextRow: ColumnValue[_]*): InsertMultiple[T] =
    InsertMultiple(table, rows ++ Seq(nextRow))

  override def add(value: ColumnValue[_]): InsertMultiple[T] = {
    val filtered = rows.map(_.filterNot(_.column == value.column))
    copy(rows = filtered.map(row => value :: row.toList))
  }
}