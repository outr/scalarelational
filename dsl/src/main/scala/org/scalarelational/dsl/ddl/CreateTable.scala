package org.scalarelational.dsl.ddl

case class CreateTable(name: String) {
  def apply(columns: CreateColumn[_]*): Unit = {
    // TODO
  }
}

case class CreateTableAndColumns(name: String, columns: Seq[CreateColumn[_]]) extends DDL {
  override def describe: String = s"CREATE TABLE $name(${columns.map(_.describe).mkString(", ")})"
}