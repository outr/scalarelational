package com.outr.query

import scala.util.matching.Regex
import org.powerscala.reflect._
import com.outr.query.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Column[T] private(val name: String)(implicit val manifest: Manifest[T], val table: Table) extends SelectExpression {
  private var _properties = Map.empty[String, ColumnProperty]
  def properties = _properties.values

  lazy val classType: EnhancedClass = manifest.runtimeClass

  lazy val longName = s"${table.tableName}.$name"

  table.addColumn(this)     // Add this column to the table

  def props(properties: ColumnProperty*) = synchronized {
    properties.foreach {
      case p => {
        _properties += p.name -> p
        p.addedTo(this)
      }
    }
    this
  }

  def has(property: ColumnProperty): Boolean = has(property.name)
  def has(propertyName: String): Boolean = _properties.contains(propertyName)
  def prop[P <: ColumnProperty](propertyName: String) = _properties.get(propertyName).asInstanceOf[Option[P]]

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

object Column {
  def apply[T](name: String, properties: ColumnProperty*)(implicit manifest: Manifest[T], table: Table) = {
    val c = new Column[T](name)
    c.props(properties: _*)
  }
}