package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Insert(values: Seq[ColumnValue[_]])

case class InsertMultiple(rows: Seq[Seq[ColumnValue[_]]])