package org.scalarelational.instruction

import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class InsertMultiple(rows: Seq[Seq[ColumnValue[_]]]) extends Insert {
  def result = {
    val datastore = rows.head.head.column.table.datastore
    datastore.exec(this)
  }
  def async = rows.head.head.column.table.datastore.async {
    result
  }
  def add(nextRow: ColumnValue[_]*) = {
    InsertMultiple(rows ++ Seq(nextRow))
  }
}