package org.scalarelational.instruction.args

import java.sql.PreparedStatement

case class IntArg(value: Int) extends SQLArgument {
  override def set(index: Int, statement: PreparedStatement): Unit = statement.setInt(index, value)
}