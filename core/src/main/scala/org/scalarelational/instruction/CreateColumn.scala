package org.scalarelational.instruction

import org.scalarelational.Database
import org.scalarelational.instruction.args.SQLArgument

case class CreateColumn[DB <: Database, T](db: DB, tableName: String, descriptor: ColumnDescriptor[T]) extends Instruction {
  override def sql: String = s"ALTER TABLE $tableName ADD ${descriptor.sql}"

  override def args: Vector[SQLArgument] = descriptor.args
}
