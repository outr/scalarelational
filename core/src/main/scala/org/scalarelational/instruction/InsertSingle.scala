package org.scalarelational.instruction

import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertSingle(values: Seq[ColumnValue[_]]) extends Insert with Instruction[Int] {
  lazy val rows = Seq(values)

  override protected def thisDatastore = values.head.column.table.datastore

  def result = {
    val datastore = values.head.column.table.datastore
    datastore.exec(this)
  }
  def and(nextRow: ColumnValue[_]*) = {
    InsertMultiple(Seq(values, nextRow))
  }
}
