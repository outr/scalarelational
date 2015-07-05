package org.scalarelational

import org.scalarelational.datatype.DataType
import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ColumnValue[T] private(val column: ColumnLike[T],
                             val value: T,
                             val converterOverride: Option[DataType[T]]) extends ExpressionValue[T] {
  def expression: ColumnLike[T] = column
  def toSQL: Any = converterOverride match {
    case Some(converter) => converter.toSQLType(column, value)
    case None => column.converter.toSQLType(column, value)
  }

  override def toString = s"$column: $value"
}

object ColumnValue {
  def apply[T](column: ColumnLike[T], value: T, converterOverride: Option[DataType[T]]): ColumnValue[T] =
    new ColumnValue[T](column, value, converterOverride)
}