package org.scalarelational.instruction.args

import java.sql.{PreparedStatement, Timestamp}

case class TimestampArg(value: Timestamp) extends SQLArgument {
  override def set(index: Int, statement: PreparedStatement): Unit = statement.setTimestamp(index, value)
}
