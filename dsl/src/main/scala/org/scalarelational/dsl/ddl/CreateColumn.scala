package org.scalarelational.dsl.ddl

case class CreateColumn[T](name: String, columnType: String, isPrimaryKey: Boolean = false) extends DDL {
  def primaryKey: CreateColumn[T] = copy(columnType, isPrimaryKey = true)

  override def describe: String = {
    val b = new StringBuilder
    b.append(columnType)
    b.append()
    b.toString()
  }
}