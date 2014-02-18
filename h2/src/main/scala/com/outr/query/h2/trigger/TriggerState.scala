package com.outr.query.h2.trigger

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TriggerState private() extends EnumEntry

object TriggerState extends Enumerated[TriggerState] {
  val Before = new TriggerState
  val After = new TriggerState
}