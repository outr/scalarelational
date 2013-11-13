package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnAlias[T](column: Column[T], alias: String) extends ColumnLike[T] {
  val name = column.name
  val longName = s"$alias.$name"
  val table = column.table
}