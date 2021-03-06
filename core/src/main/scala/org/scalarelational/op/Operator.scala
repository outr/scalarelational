package org.scalarelational.op

import enumeratum._

sealed abstract class Operator(val symbol: String) extends EnumEntry

object Operator extends Enum[Operator] {
  case object Equal extends Operator("=")
  case object NotEqual extends Operator("!=")
  case object GreaterThan extends Operator(">")
  case object LessThan extends Operator("<")
  case object GreaterThanOrEqual extends Operator(">=")
  case object LessThanOrEqual extends Operator("<=")
  case object Between extends Operator("BETWEEN")
  case object Like extends Operator("LIKE")
  case object In extends Operator("IN")
  case object Is extends Operator("IS")
  case object IsNot extends Operator("IS NOT")

  val values = findValues.toVector
}