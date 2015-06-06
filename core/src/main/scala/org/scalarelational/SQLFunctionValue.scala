package org.scalarelational

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class SQLFunctionValue[T](function: SQLFunction[T], value: T) extends ExpressionValue[T] {
  def expression = function
}