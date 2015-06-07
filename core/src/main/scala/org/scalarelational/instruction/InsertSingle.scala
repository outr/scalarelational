package org.scalarelational.instruction

import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertSingle(values: Seq[ColumnValue[_]]) extends Insert with Instruction[Int] {
  lazy val rows = Seq(values)

  def result = {
    val datastore = values.head.column.table.datastore
    datastore.exec(this)
  }
  def async = values.head.column.table.datastore.async {
    result
  }
  def add(nextRow: ColumnValue[_]*) = {
    InsertMultiple(Seq(values, nextRow))
  }
}
