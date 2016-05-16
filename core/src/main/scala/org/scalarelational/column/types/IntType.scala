package org.scalarelational.column.types

import org.scalarelational.instruction.args.{IntArg, SQLArgument}

object IntType extends ColumnType[Int] {
  override def apply(value: Int): SQLArgument = IntArg(value)

  override def sql: String = "INTEGER"

  override def args: Vector[SQLArgument] = Vector.empty
}
