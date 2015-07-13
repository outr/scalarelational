package org.scalarelational

import org.scalarelational.model.Datastore

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CallableInstruction(sql: String) {
  def execute(datastore: Datastore): Unit = {
    val s = datastore.connection.prepareCall(sql)
    try {
      s.execute()
    } finally {
      s.close()
    }
  }
}