package org.scalarelational.h2.trigger

import org.scalarelational.{Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TriggerEvent(table: Table, triggerType: TriggerType, state: TriggerState, oldRow: Array[AnyRef], newRow: Array[AnyRef]) {
  def apply[T](column: Column[T], array: Array[AnyRef] = defaultArray) = column.converter.fromSQLType(column, defaultArray(column.index))

  def defaultArray = if (triggerType == TriggerType.Delete) {
    oldRow
  } else {
    newRow
  }
}
