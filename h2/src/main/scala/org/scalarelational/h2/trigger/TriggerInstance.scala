package org.scalarelational.h2.trigger

import java.sql.Connection

import org.h2.api.Trigger
import org.scalarelational.h2.H2Database
import org.scalarelational.model.Database
import org.scalarelational.table.Table

class TriggerInstance extends Trigger {
  private var table: Table = _
  private var triggerType: TriggerType = _
  private var triggerState: TriggerState = _
  private def database = table.database.asInstanceOf[H2Database]

  override def init(conn: Connection, schemaName: String, triggerName: String, tableName: String, before: Boolean, `type`: Int): Unit = {
    Database().tableByName(tableName) match {
      case Some(t) => table = t
      case None => throw new RuntimeException(s"Unable to find $tableName in ${Database()}.")
    }
    triggerState = if (before) TriggerState.Before else TriggerState.After
    triggerType = `type` match {
      case 1 => TriggerType.Insert
      case 2 => TriggerType.Update
      case 4 => TriggerType.Delete
      case 8 => TriggerType.Select
    }
  }

  override def fire(conn: Connection, oldRow: Array[AnyRef], newRow: Array[AnyRef]): Unit =
    database.trigger := TriggerEvent(table, triggerType, triggerState, oldRow, newRow)

  override def remove(): Unit = {}

  override def close(): Unit = {}
}