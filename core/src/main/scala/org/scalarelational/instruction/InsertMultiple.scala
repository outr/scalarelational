package org.scalarelational.instruction

import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertMultiple(rows: Seq[Seq[ColumnValue[_]]]) extends Insert with Instruction[List[Int]] {
  override def table = rows.head.head.column.table

  def result = thisDatastore.exec(this)
  def and(nextRow: ColumnValue[_]*) = {
    InsertMultiple(rows ++ Seq(nextRow))
  }
}