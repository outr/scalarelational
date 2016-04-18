package org.scalarelational.dsl

package object ddl {
  implicit class CreateColumnAttributes[T](cc: CreateColumnEntry[T]) {
    def primaryKey: CreateColumnEntry[T] = cc.withAttribute(ColumnAttribute[T]("PRIMARY KEY"))
    def autoIncrement: CreateColumnEntry[T] = cc.withAttribute(ColumnAttribute[T]("AUTO_INCREMENT"))
    def notNull: CreateColumnEntry[T] = cc.withAttribute(ColumnAttribute[T]("NOT NULL"))
    def unique: CreateColumnEntry[T] = cc.withAttribute(ColumnAttribute[T]("UNIQUE"))
  }
}