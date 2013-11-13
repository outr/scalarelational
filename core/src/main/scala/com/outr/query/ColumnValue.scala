package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnValue[T](column: ColumnLike[T], value: T) extends ExpressionValue[T] {
  def expression = column
}