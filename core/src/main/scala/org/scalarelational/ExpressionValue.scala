package org.scalarelational

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ExpressionValue[T] {
  def expression: SelectExpression[T]
  def value: T
}