package org.scalarelational.model

import org.scalarelational._
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.fun.{SQLFunction, FunctionType}
import org.scalarelational.op._
import org.scalarelational.datatype.{LongDataType, DataType}

import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnLike[T] extends SelectExpression[T] with ColumnPropertyContainer {
  def name: String
  def longName: String
  def table: Table
  def dataType: DataType[T]
  def manifest: Manifest[T]

  def sqlType = dataType.sqlType(table.datastore, this)

  def apply(value: T, converterOverride: Option[DataType[T]] = None): ColumnValue[T] =
    ColumnValue[T](this, value, converterOverride)

  def value(v: Any): T = {
    val toConvert = v match {
      case cv: ColumnValue[_] => cv.toSQL
      case _ => v
    }

    try {
      toConvert.asInstanceOf[T]
    } catch {
      case t: Throwable =>
        val sourceClass = manifest.runtimeClass
        val targetClass = v.getClass
        throw new RuntimeException(s"Invalid conversion from $sourceClass to $targetClass (table = $table, column = $this, value = $toConvert)")
    }
  }

  def ===(value: T) = DirectCondition(this, Operator.Equal, value)
  def !==(value: T) = DirectCondition(this, Operator.NotEqual, value)
  def <>(value: T) = DirectCondition(this, Operator.NotEqual, value)
  def >(value: T) = DirectCondition(this, Operator.GreaterThan, value)
  def <(value: T) = DirectCondition(this, Operator.LessThan, value)
  def >=(value: T) = DirectCondition(this, Operator.GreaterThanOrEqual, value)
  def <=(value: T) = DirectCondition(this, Operator.LessThanOrEqual, value)
  def between(range: Seq[T]) = RangeCondition(this, Operator.Between, range)
  def %(pattern: String) = LikeCondition(this, pattern, not = false)
  def like(pattern: String) = LikeCondition(this, pattern, not = false)
  def !%(pattern: String) = LikeCondition(this, pattern, not = true)
  def notLike(pattern: String) = LikeCondition(this, pattern, not = true)
  def *(regex: Regex) = RegexCondition(this, regex, not = false)
  def regex(regex: Regex) = RegexCondition(this, regex, not = false)
  def !*(regex: Regex) = RegexCondition(this, regex, not = true)
  def notRegex(regex: Regex) = RegexCondition(this, regex, not = true)
  def in(range: Seq[T]) = RangeCondition(this, Operator.In, range)

  def ===(column: ColumnLike[T]) = ColumnCondition(this, Operator.Equal, column)

  def avg = SQLFunction[T](FunctionType.Avg, this, dataType)
  def count = SQLFunction[Long](FunctionType.Count, this, LongDataType)
  def min = SQLFunction[T](FunctionType.Min, this, dataType)
  def max = SQLFunction[T](FunctionType.Max, this, dataType)
  def sum = SQLFunction[T](FunctionType.Sum, this, dataType)
}
