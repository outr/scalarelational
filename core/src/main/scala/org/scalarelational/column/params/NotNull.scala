package org.scalarelational.column.params
import org.scalarelational.instruction.args.SQLArgument

object NotNull extends ColumnParam {
  override def sql: String = "NOT NULL"

  override def args: Vector[SQLArgument] = Vector.empty
}
