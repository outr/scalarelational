package org.scalarelational.instruction

import org.scalarelational.Session
import org.scalarelational.model.{Database, SQLContainer}

case class CallableInstruction(sql: String) {
  def execute(database: Database)(implicit session: Session): Unit = {
    SQLContainer.calling(database, InstructionType.DDL, sql)
    val s = session.connection.prepareCall(sql)
    try {
      s.execute()
    } finally {
      s.close()
    }
  }
}