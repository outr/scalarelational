package org.scalarelational.instruction

import java.sql.SQLException

import org.scalarelational.Session
import org.scalarelational.model.{Datastore, SQLContainer}

case class CallableInstruction(sql: String) {
  def execute(datastore: Datastore)(implicit session: Session): Unit = {
    SQLContainer.calling(datastore, InstructionType.DDL, sql)
    val s = session.connection.prepareCall(sql)
    try {
      s.execute()
    } catch {
      case t: Throwable => throw new SQLException(s"Exception thrown while executing sql: $sql", t)
    } finally {
      s.close()
    }
  }
}