package org.scalarelational

import java.sql.Timestamp

import org.scalarelational.column.params.{AutoIncrement, NotNull, PrimaryKey, Unique}
import org.scalarelational.column.types.{IntType, TimestampType, VarCharType}
import org.scalarelational.instruction.{ColumnDescriptor, CreateTable}

package object dsl {
  implicit class DatabaseDSLSupport[D <: Database](database: D) {
    object create {
      def table(name: String, ifNotExists: Boolean = false)(columns: ColumnDescriptor[_]*): CreateTable[D] = {
        CreateTable(database, name, ifNotExists, columns: _*)
      }
    }
  }
  implicit class BasicColumnTypes(name: String) {
    def integer: ColumnDescriptor[Int] = ColumnDescriptor(name, IntType, Nil)
    def varchar(size: Int): ColumnDescriptor[String] = ColumnDescriptor(name, VarCharType(size), Nil)
    def timestamp: ColumnDescriptor[Timestamp] = ColumnDescriptor(name, TimestampType, Nil)
  }
  implicit class BasicParams[T](descriptor: ColumnDescriptor[T]) {
    def primaryKey: ColumnDescriptor[T] = descriptor.withParam(PrimaryKey)
    def autoIncrement: ColumnDescriptor[T] = descriptor.withParam(AutoIncrement)
    def notNull: ColumnDescriptor[T] = descriptor.withParam(NotNull)
    def unique: ColumnDescriptor[T] = descriptor.withParam(Unique)
  }
}