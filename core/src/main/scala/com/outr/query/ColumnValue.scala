package com.outr.query

import com.outr.query.convert.ColumnConverter

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnValue[T](column: ColumnLike[T],
                          value: T,
                          converterOverride: Option[ColumnConverter[T]]) extends ExpressionValue[T] {
  def expression = column
  def toSQL = converterOverride match {
    case Some(converter) => converter.toSQLType(column, value)
    case None => column.converter.toSQLType(column, value)
  }
}