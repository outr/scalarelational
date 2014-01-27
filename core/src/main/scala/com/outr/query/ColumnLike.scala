package com.outr.query

import scala.util.matching.Regex
import com.outr.query.convert.ColumnConverter
import org.powerscala.reflect.EnhancedMethod

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnLike[T] extends SelectExpression {
  def name: String
  def longName: String
  def table: Table
  def converter: ColumnConverter[T]
  def manifest: Manifest[T]

  def sqlType = converter.sqlType(this)

  def apply(value: T, converterOverride: Option[ColumnConverter[T]] = None) = ColumnValue[T](this, value, converterOverride)
  def value(v: Any) = {
    val toConvert = v match {
      case cv: ColumnValue[_] => cv.toSQL
      case _ => v
    }
    try {
      val value = EnhancedMethod.convertTo(name, toConvert, manifest.runtimeClass).asInstanceOf[T]
      println(s"$toConvert converted to $value (${manifest.runtimeClass} - $longName)")
      apply(value)
    } catch {
      case t: Throwable => {
        throw new RuntimeException(s"Name: $name, Value: $v, toConvert: $toConvert, Class: ${manifest.runtimeClass}", t)
      }
    }
  }

  def isNull = NullCondition(this, Operator.Is)
  def isNotNull = NullCondition(this, Operator.IsNot)
  def ===(value: T) = DirectCondition(this, Operator.Equal, value)
  def <>(value: T) = DirectCondition(this, Operator.NotEqual, value)
  def !=(value: T) = DirectCondition(this, Operator.NotEqual, value)
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

  def avg = SimpleFunction[T](FunctionType.Avg, this)
  def count = SimpleFunction[Long](FunctionType.Count, this)
  def min = SimpleFunction[T](FunctionType.Min, this)
  def max = SimpleFunction[T](FunctionType.Max, this)
  def sum = SimpleFunction[T](FunctionType.Sum, this)
}
