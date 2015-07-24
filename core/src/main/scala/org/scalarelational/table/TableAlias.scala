package org.scalarelational.table

import scala.language.existentials

import org.scalarelational.column.{ColumnAlias, ColumnLike}
import org.scalarelational.instruction.Joinable

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TableAlias(table: Table[_], tableAlias: String) extends Joinable {
  def apply[T](column: ColumnLike[T]) = ColumnAlias[T](column, Option(tableAlias), Option(column.name), None)
}
