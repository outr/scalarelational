package org.scalarelational.fun

import org.scalarelational.ExpressionValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class SQLFunctionValue[T](function: SQLFunction[T], value: T) extends ExpressionValue[T] {
  def expression = function

  override def toString = s"${function.functionType.sql}(alias = ${function.alias}) = $value"
}