package org.scalarelational.instruction

import org.scalarelational.Database
import org.scalarelational.instruction.args.SQLArgument

case class CreateTable[DB <: Database](db: DB, name: String, ifNotExists: Boolean, columns: ColumnDescriptor[_]*) extends Instruction {
  override def sql: String = if (ifNotExists) {
    s"CREATE TABLE IF NOT EXISTS $name(${columns.map(_.sql).mkString(", ")})"
  } else {
    s"CREATE TABLE $name(${columns.map(_.sql).mkString(", ")})"
  }

  override def args: Vector[SQLArgument] = mergeArgs(columns: _*)
}
