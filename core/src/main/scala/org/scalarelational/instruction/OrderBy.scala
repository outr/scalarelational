package org.scalarelational.instruction

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.scalarelational.SelectExpression


case class OrderBy[T](expression: SelectExpression[T], direction: OrderDirection)

sealed abstract class OrderDirection(val sql: String) extends EnumEntry

object OrderDirection extends Enumerated[OrderDirection] {
  case object Ascending extends OrderDirection("ASC")
  case object Descending extends OrderDirection("DESC")

  val values = findValues.toVector
}