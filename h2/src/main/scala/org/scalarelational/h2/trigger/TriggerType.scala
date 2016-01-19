package org.scalarelational.h2.trigger

import enumeratum._

sealed abstract class TriggerType extends EnumEntry {
  def is(types: TriggerType*): Boolean = types.contains(this)
}

object TriggerType extends Enum[TriggerType] {
  case object Insert extends TriggerType
  case object Update extends TriggerType
  case object Delete extends TriggerType
  case object Select extends TriggerType

  val values = findValues.toVector
}