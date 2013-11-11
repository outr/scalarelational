package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SelectExpression {
  def asc = OrderBy(this, OrderDirection.Ascending)
  def desc = OrderBy(this, OrderDirection.Descending)
}