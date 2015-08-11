package org.scalarelational.h2.trigger

import org.scalarelational.column.Column
import org.scalarelational.datatype.SQLConversion
import org.scalarelational.table.Table

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TriggerEvent(table: Table, triggerType: TriggerType, state: TriggerState, oldRow: Array[AnyRef], newRow: Array[AnyRef]) {
  def apply[T](column: Column[T], array: Array[AnyRef] = defaultArray) = column.dataType.converter.asInstanceOf[SQLConversion[T, AnyRef]].fromSQL(column, defaultArray(column.index))

  def defaultArray = if (triggerType == TriggerType.Delete) {
    oldRow
  } else {
    newRow
  }
}
