package org.scalarelational.h2.trigger

import enumeratum._

sealed abstract class TriggerState extends EnumEntry

object TriggerState extends Enum[TriggerState] {
  case object Before extends TriggerState
  case object After extends TriggerState

  val values = findValues.toVector
}