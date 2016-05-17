package org.scalarelational

import java.sql.Timestamp

import org.scalarelational.column.params.{AutoIncrement, NotNull, PrimaryKey, Unique}
import org.scalarelational.column.types.{IntType, TimestampType, VarCharType}
import org.scalarelational.instruction.{ColumnDescriptor, CreateTable}

package object dsl {
  implicit class DatabaseDSLSupport[D <: Database](database: D) {
    object create {
      def table(name: String, columns: ColumnDescriptor[_]*): CreateTable[D] = {
        CreateTable(database, name, false, columns: _*)
      }
      def tableIfNotExists(name: String, columns: ColumnDescriptor[_]*): CreateTable[D] = {
        CreateTable(database, name, true, columns: _*)
      }
    }
  }
  object integer {
    def apply(name: String): ColumnDescriptor[Int] = ColumnDescriptor(name, IntType, Nil)
  }
  object varchar {
    def apply(name: String, size: Int): ColumnDescriptor[String] = ColumnDescriptor(name, VarCharType(size), Nil)
  }
  object timestamp {
    def apply(name: String): ColumnDescriptor[Timestamp] = ColumnDescriptor(name, TimestampType, Nil)
  }
  implicit class BasicParams[T](descriptor: ColumnDescriptor[T]) {
    def primaryKey: ColumnDescriptor[T] = descriptor.withParam(PrimaryKey)
    def autoIncrement: ColumnDescriptor[T] = descriptor.withParam(AutoIncrement)
    def notNull: ColumnDescriptor[T] = descriptor.withParam(NotNull)
    def unique: ColumnDescriptor[T] = descriptor.withParam(Unique)
  }
}