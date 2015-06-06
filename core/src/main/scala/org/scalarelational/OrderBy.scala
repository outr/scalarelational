package org.scalarelational

import org.powerscala.enum.{EnumEntry, Enumerated}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class OrderBy(expression: SelectExpression, direction: OrderDirection)

sealed abstract class OrderDirection(val sql: String) extends EnumEntry

object OrderDirection extends Enumerated[OrderDirection] {
  case object Ascending extends OrderDirection("ASC")
  case object Descending extends OrderDirection("DESC")

  val values = findValues.toVector
}