package org.scalarelational.dsl

package object ddl {
  def create: Create = Create

  implicit class ColumnName(name: String) {
    def int: CreateColumn[Int] = CreateColumn[Int](name, "INT")
    def varchar(length: Int): CreateColumn[String] = CreateColumn[String](name, s"VARCHAR($length)")
  }

  implicit class CreateColumnAttributes[T](cc: CreateColumn[T]) {
    def primaryKey: CreateColumn[T] = cc.withAttribute(ColumnAttribute[T]("PRIMARY KEY"))
  }
}