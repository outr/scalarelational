package org.scalarelational.table

import org.scalarelational.column.{ColumnAlias, ColumnLike}
import org.scalarelational.instruction.Joinable

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TableAlias(table: Table, tableAlias: String) extends Joinable {
  def apply[T](column: ColumnLike[T]) = ColumnAlias[T](column, Option(tableAlias), Option(column.name), None)
}
