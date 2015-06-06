package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Insert {
  def rows: Seq[Seq[ColumnValue[_]]]
}

case class InsertSingle(values: Seq[ColumnValue[_]]) extends Insert {
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