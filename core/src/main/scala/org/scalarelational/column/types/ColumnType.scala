package org.scalarelational.column.types

import org.scalarelational.instruction.SQLPart
import org.scalarelational.instruction.args.SQLArgument

trait ColumnType[T] extends SQLPart {
  def apply(value: T): SQLArgument
}