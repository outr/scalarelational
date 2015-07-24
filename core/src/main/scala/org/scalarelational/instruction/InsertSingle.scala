package org.scalarelational.instruction

import org.scalarelational.datatype.Ref
import org.scalarelational.column.ColumnValue
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertSingle[T](table: Table[T], values: Seq[ColumnValue[_]])
  extends Insert with Instruction[T, Ref[T]] {
  lazy val rows = Seq(values)

  def result: Ref[T] = new Ref[T](table.datastore.exec(this))

  def and(nextRow: ColumnValue[_]*): InsertMultiple[T] =
    InsertMultiple(table, Seq(values, nextRow))

  override def add(value: ColumnValue[_]): InsertSingle[T] = {
    val filtered = values.filterNot(_.column == value.column)
    copy(values = value :: filtered.toList)
  }
}