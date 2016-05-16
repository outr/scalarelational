package org.scalarelational.column.params

import org.scalarelational.instruction.args.SQLArgument

object PrimaryKey extends ColumnParam {
  override def sql: String = "PRIMARY KEY"

  override def args: Vector[SQLArgument] = Vector.empty
}