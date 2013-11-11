package com.outr.query

import scala.util.matching.Regex
import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Column[T](name: String,
                     notNull: Boolean = false,
                     autoIncrement: Boolean = false,
                     primaryKey: Boolean = false,
                     unique: Boolean = false,
                     foreignKey: Option[Column[T]] = None)
                    (implicit val manifest: Manifest[T], val table: Table) extends SelectExpression {
  lazy val classType: EnhancedClass = manifest.runtimeClass

  lazy val longName = s"${table.tableName}.$name"

  table.addColumn(this)     // Add this column to the table
  foreignKey match {
    case Some(foreign) => {
      val foreignTable = foreign.table
      foreignTable.addForeignColumn(this)
    }
    case None => // Nothing to do
  }

  def apply(value: T) = ColumnValue[T](this, value)

  def ===(value: T) = DirectCondition(this, Operator.Equal, value)
  def <>(value: T) = DirectCondition(this, Operator.NotEqual, value)
  def !=(value: T) = DirectCondition(this, Operator.NotEqual, value)
  def >(value: T) = DirectCondition(this, Operator.GreaterThan, value)
  def <(value: T) = DirectCondition(this, Operator.LessThan, value)
  def >=(value: T) = DirectCondition(this, Operator.GreaterThanOrEqual, value)
  def <=(value: T) = DirectCondition(this, Operator.LessThanOrEqual, value)
  def between(range: Seq[T]) = RangeCondition(this, Operator.Between, range)
  def like(regex: Regex) = LikeCondition(this, regex)
  def in(range: Seq[T]) = RangeCondition(this, Operator.In, range)

  def ===(column: Column[T]) = ColumnCondition(this, Operator.Equal, column)

  def avg = SimpleFunction[T](FunctionType.Avg, this)
  def count = SimpleFunction[Long](FunctionType.Count, this)
  def min = SimpleFunction[T](FunctionType.Min, this)
  def max = SimpleFunction[T](FunctionType.Max, this)
  def sum = SimpleFunction[T](FunctionType.Sum, this)

  override def toString = s"Column(${table.tableName}.$name)"
}