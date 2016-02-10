package org.scalarelational.dsl.ddl

case class CreateColumn[T](name: String, columnType: String, isPrimaryKey: Boolean = false) extends DDL {
  def primaryKey: CreateColumn[T] = copy(isPrimaryKey = true)

  override def describe: String = {
    val b = new StringBuilder
    b.append(s"$name $columnType")
    if (isPrimaryKey) {
      b.append(s" PRIMARY KEY")
    }
    b.toString()
  }
}