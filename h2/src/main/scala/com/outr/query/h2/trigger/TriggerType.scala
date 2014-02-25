package com.outr.query.h2.trigger

import org.powerscala.enum.{Enumerated, EnumEntry}

class TriggerType private() extends EnumEntry {
  def is(types: TriggerType*) = types.contains(this)
}

object TriggerType extends Enumerated[TriggerType] {
  val Insert = new TriggerType
  val Update = new TriggerType
  val Delete = new TriggerType
  val Select = new TriggerType
}