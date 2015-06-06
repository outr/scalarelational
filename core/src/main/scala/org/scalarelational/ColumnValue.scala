package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.datatype.DataType

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnValue[T](column: ColumnLike[T],
                          value: T,
                          converterOverride: Option[DataType[T]]) extends ExpressionValue[T] {
  if (value.asInstanceOf[AnyRef] != null && !value.getClass.hasType(column.manifest.runtimeClass)) {
    throw new RuntimeException(s"Unable to set column-value $value (${value.getClass.getName}) when ${column.manifest.runtimeClass.getName} is expected in ${column.longName}.")
  }

  def expression = column
  def toSQL = converterOverride match {
    case Some(converter) => converter.toSQLType(column, value)
    case None => column.converter.toSQLType(column, value)
  }

  override def toString = s"$column = $value"
}