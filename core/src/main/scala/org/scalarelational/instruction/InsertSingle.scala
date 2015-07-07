package org.scalarelational.instruction

import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertSingle(values: Seq[ColumnValue[_]]) extends Insert with Instruction[Int] {
  lazy val rows = Seq(values)

  def table = values.head.column.table

  def result = {
    thisDatastore.exec(this)
  }
  def and(nextRow: ColumnValue[_]*) = {
    InsertMultiple(Seq(values, nextRow))
  }

  override def add(value: ColumnValue[_]): InsertSingle = {
    val filtered = values.filterNot(_.column == value.column)
    copy(values = value :: filtered.toList)
  }
}