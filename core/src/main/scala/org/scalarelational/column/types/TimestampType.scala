package org.scalarelational.column.types

import java.sql.Timestamp

import org.scalarelational.instruction.args.{SQLArgument, TimestampArg}

object TimestampType extends ColumnType[Timestamp] {
  override def apply(value: Timestamp): SQLArgument = TimestampArg(value)

  override def sql: String = "TIMESTAMP"

  override def args: Vector[SQLArgument] = Vector.empty
}
