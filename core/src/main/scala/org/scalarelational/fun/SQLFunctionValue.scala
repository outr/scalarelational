package org.scalarelational.fun

import org.scalarelational.ExpressionValue


case class SQLFunctionValue[T, S](function: SQLFunction[T, S], value: T) extends ExpressionValue[T] {
  def expression: SQLFunction[T, S] = function

  override def toString: String = s"${function.functionType.sql}(alias = ${function.alias}) = $value"
}