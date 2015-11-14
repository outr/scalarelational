package org.scalarelational.instruction

import org.powerscala.enum.{EnumEntry, Enumerated}


sealed abstract class ConnectType extends EnumEntry

object ConnectType extends Enumerated[ConnectType] {
  case object And extends ConnectType
  case object Or extends ConnectType

  val values = findValues.toVector
}