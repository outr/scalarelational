package org.scalarelational.instruction

import enumeratum._

sealed abstract class ConnectType extends EnumEntry

object ConnectType extends Enum[ConnectType] {
  case object And extends ConnectType
  case object Or extends ConnectType

  val values = findValues.toVector
}