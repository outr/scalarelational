package org.scalarelational.table

import org.scalarelational.column.{ColumnAlias, ColumnLike}
import org.scalarelational.instruction.Joinable

import scala.language.existentials

case class TableAlias(table: Table, tableAlias: String) extends Joinable {
  def apply[T, S](column: ColumnLike[T, S]): ColumnAlias[T, S] =
    ColumnAlias[T, S](column, Option(tableAlias), Option(column.name), None)
}
