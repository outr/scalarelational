package org.scalarelational.h2.trigger

import java.sql.Connection

import org.h2.api.Trigger

import org.powerscala.log.Logging

import org.scalarelational.table.Table
import org.scalarelational.model.Datastore
import org.scalarelational.h2.H2Datastore

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TriggerInstance extends Trigger with Logging {
  private var table: Table[_] = _
  private var triggerType: TriggerType = _
  private var triggerState: TriggerState = _
  private def datastore = table.datastore.asInstanceOf[H2Datastore]

  override def init(conn: Connection, schemaName: String, triggerName: String, tableName: String, before: Boolean, `type`: Int) = {
    Datastore().tableByName(tableName) match {
      case Some(t) => table = t
      case None => throw new RuntimeException(s"Unable to find $tableName in ${Datastore()}.")
    }
    triggerState = if (before) TriggerState.Before else TriggerState.After
    triggerType = `type` match {
      case 1 => TriggerType.Insert
      case 2 => TriggerType.Update
      case 4 => TriggerType.Delete
      case 8 => TriggerType.Select
    }
  }

  override def fire(conn: Connection, oldRow: Array[AnyRef], newRow: Array[AnyRef]) = {
    datastore.trigger.fire(TriggerEvent(table, triggerType, triggerState, oldRow, newRow))
  }

  override def remove() = {}

  override def close() = {}
}