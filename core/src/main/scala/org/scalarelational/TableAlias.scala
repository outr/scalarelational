package org.scalarelational

import org.scalarelational.instruction.Joinable
import org.scalarelational.model.{Table, ColumnLike, ColumnAlias}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TableAlias(table: Table, tableAlias: String) extends Joinable {
  def apply[T](column: ColumnLike[T]) = ColumnAlias[T](column, tableAlias, column.name)
}
