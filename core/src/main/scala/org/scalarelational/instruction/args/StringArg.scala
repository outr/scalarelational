package org.scalarelational.instruction.args

import java.sql.PreparedStatement

case class StringArg(value: String) extends SQLArgument {
  override def set(index: Int, statement: PreparedStatement): Unit = statement.setString(index, value)
}