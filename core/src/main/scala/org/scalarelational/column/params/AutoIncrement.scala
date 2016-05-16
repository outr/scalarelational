package org.scalarelational.column.params
import org.scalarelational.instruction.args.SQLArgument

object AutoIncrement extends ColumnParam {
  override def sql: String = "AUTO_INCREMENT"

  override def args: Vector[SQLArgument] = Vector.empty
}
