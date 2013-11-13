package com.outr.query

import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnLike[T] extends SelectExpression {
  def name: String
  def longName: String
  def table: Table

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

  def ===(column: ColumnLike[T]) = ColumnCondition(this, Operator.Equal, column)

  def avg = SimpleFunction[T](FunctionType.Avg, this)
  def count = SimpleFunction[Long](FunctionType.Count, this)
  def min = SimpleFunction[T](FunctionType.Min, this)
  def max = SimpleFunction[T](FunctionType.Max, this)
  def sum = SimpleFunction[T](FunctionType.Sum, this)
}
