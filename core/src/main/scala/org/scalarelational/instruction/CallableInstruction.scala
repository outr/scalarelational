package org.scalarelational.instruction

import org.scalarelational.model.{SQLContainer, Datastore}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CallableInstruction(sql: String) {
  def execute(datastore: Datastore): Unit = {
    SQLContainer.calling(datastore, InstructionType.DDL, sql)
    val s = datastore.connection.prepareCall(sql)
    try {
      s.execute()
    } finally {
      s.close()
    }
  }
}