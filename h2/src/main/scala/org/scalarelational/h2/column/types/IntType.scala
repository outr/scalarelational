package org.scalarelational.h2.column.types

import org.scalarelational.column.Column
import org.scalarelational.column.types.ColumnType

case class IntType(columnName: Option[String] = None,
                   defaultValue: Option[Int] = None,
                   isPrimaryKey: Boolean = false,
                   isAutoIncrement: Boolean = false,
                   foreignColumn: Option[() => Column[Int]] = None) extends ColumnType[Int] {
  def name(name: String): IntType = copy(columnName = Option(name))
  def primaryKey: IntType = copy(isPrimaryKey = true)
  def autoIncrement: IntType = copy(isAutoIncrement = true)
  def foreignKey(f: => Column[Int]): IntType = copy(foreignColumn = Option(() => f))
  def default(value: Int): IntType = copy(defaultValue = Some(value))
}

object IntType extends IntType(None, None, false, false, None)