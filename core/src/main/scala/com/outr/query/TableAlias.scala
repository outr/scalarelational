package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TableAlias(table: Table, alias: String) {
  def apply[T](column: Column[T]) = ColumnAlias[T](column, alias)
}
