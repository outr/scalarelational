package org.scalarelational.h2.trigger

import org.powerscala.enum.{EnumEntry, Enumerated}

sealed abstract class TriggerType extends EnumEntry {
  def is(types: TriggerType*) = types.contains(this)
}

object TriggerType extends Enumerated[TriggerType] {
  case object Insert extends TriggerType
  case object Update extends TriggerType
  case object Delete extends TriggerType
  case object Select extends TriggerType

  val values = findValues.toVector
}