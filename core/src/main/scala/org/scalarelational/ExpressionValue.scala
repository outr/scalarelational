package org.scalarelational


trait ExpressionValue[T] {
  def expression: SelectExpression[T]
  def value: T
}