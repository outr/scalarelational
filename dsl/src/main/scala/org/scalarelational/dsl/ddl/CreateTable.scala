package org.scalarelational.dsl.ddl

case class CreateTable(name: String, ifNotExists: Boolean) {
  def apply(columns: CreateColumn[_]*): CreateTableAndColumns = CreateTableAndColumns(name, ifNotExists, columns)
}

case class CreateTableAndColumns(name: String, ifNotExists: Boolean, columns: Seq[CreateColumn[_]]) extends DDL {
  override def describe: String = s"CREATE TABLE${if (ifNotExists) " IF NOT EXISTS" else ""} $name(${columns.map(_.describe).mkString(", ")})"
}