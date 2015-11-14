package org.scalarelational.h2.trigger

import org.scalarelational.column.Column
import org.scalarelational.table.Table

import scala.language.existentials


case class TriggerEvent(table: Table, triggerType: TriggerType, state: TriggerState, oldRow: Array[AnyRef], newRow: Array[AnyRef]) {
  def apply[T, S](column: Column[T, S], array: Array[AnyRef] = defaultArray) = column.dataType.converter.fromSQL(column, defaultArray(column.index).asInstanceOf[S])

  def defaultArray = if (triggerType == TriggerType.Delete) {
    oldRow
  } else {
    newRow
  }
}
