package org.scalarelational.dsl.ddl

import org.scalarelational.instruction.Instruction
import org.scalarelational.{Database, Session}

case class CreateTable[D <: Database](database: D, name: String, ifNotExists: Boolean) {
  def apply(columns: CreateColumnEntry[_]*): CreateTableAndColumns[D] = CreateTableAndColumns(database, name, ifNotExists, columns)
}

case class CreateTableAndColumns[D <: Database](database: D,
                                                name: String,
                                                ifNotExists: Boolean,
                                                columns: Seq[CreateColumnEntry[_]]) extends Instruction[D, Boolean] {
  override def describe: String = s"CREATE TABLE${if (ifNotExists) " IF NOT EXISTS" else ""} $name(${columns.map(_.describe).mkString(", ")})"

  override def args: Vector[Any] = Vector.empty

  override def exec()(implicit session: Session[D]): Boolean = call(session)
}