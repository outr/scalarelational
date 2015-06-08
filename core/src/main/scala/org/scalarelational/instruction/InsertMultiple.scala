package org.scalarelational.instruction

import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertMultiple(rows: Seq[Seq[ColumnValue[_]]]) extends Insert with Instruction[List[Int]] {
  override protected def thisDatastore = rows.head.head.column.table.datastore

  def result = thisDatastore.exec(this)
  def add(nextRow: ColumnValue[_]*) = {
    InsertMultiple(rows ++ Seq(nextRow))
  }
}