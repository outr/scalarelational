package org.scalarelational

import org.scalarelational.instruction.{OrderBy, OrderDirection}

trait SelectExpression[T] {
  def asc = OrderBy(this, OrderDirection.Ascending)
  def desc = OrderBy(this, OrderDirection.Descending)
  def toSQL: String
}