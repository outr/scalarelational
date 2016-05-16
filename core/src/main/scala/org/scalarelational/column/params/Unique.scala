package org.scalarelational.column.params
import org.scalarelational.instruction.args.SQLArgument

object Unique extends ColumnParam {
  override def sql: String = "UNIQUE"

  override def args: Vector[SQLArgument] = Vector.empty
}
