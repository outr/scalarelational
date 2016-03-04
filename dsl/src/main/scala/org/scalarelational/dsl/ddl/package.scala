package org.scalarelational.dsl

import java.sql.Timestamp

package object ddl {
  def create: Create = Create

  implicit class ColumnName(name: String) {
    def int: CreateColumn[Int] = CreateColumn[Int](name, "INTEGER")
    def varchar(length: Int): CreateColumn[String] = CreateColumn[String](name, s"VARCHAR($length)")
    def timestamp: CreateColumn[Timestamp] = CreateColumn[Timestamp](name, "TIMESTAMP")
  }

  implicit class CreateColumnAttributes[T](cc: CreateColumn[T]) {
    def primaryKey: CreateColumn[T] = cc.withAttribute(ColumnAttribute[T]("PRIMARY KEY"))
    def autoIncrement: CreateColumn[T] = cc.withAttribute(ColumnAttribute[T]("AUTO_INCREMENT"))
    def notNull: CreateColumn[T] = cc.withAttribute(ColumnAttribute[T]("NOT NULL"))
    def unique: CreateColumn[T] = cc.withAttribute(ColumnAttribute[T]("UNIQUE"))
  }
}