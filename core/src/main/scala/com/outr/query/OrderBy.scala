package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class OrderBy(expression: SelectExpression, direction: OrderDirection)

class OrderDirection private(val sql: String) extends EnumEntry

object OrderDirection extends Enumerated[OrderDirection] {
  val Ascending = new OrderDirection("ASC")
  val Descending = new OrderDirection("DESC")
}