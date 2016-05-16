package org.scalarelational.column.types

import org.scalarelational.instruction.args.{SQLArgument, StringArg}

case class VarCharType(size: Int) extends ColumnType[String] {
  override def apply(value: String): SQLArgument = StringArg(value)

  override def sql: String = s"VARCHAR($size)"

  override def args: Vector[SQLArgument] = Vector.empty
}
