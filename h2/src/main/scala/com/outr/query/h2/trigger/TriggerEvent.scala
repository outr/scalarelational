package com.outr.query.h2.trigger

import com.outr.query.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TriggerEvent(table: Table, triggerType: TriggerType, state: TriggerState, oldRow: Array[AnyRef], newRow: Array[AnyRef])
