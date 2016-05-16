package org.scalarelational.instruction.args

import java.sql.PreparedStatement

/**
  * SQLArgument is able to set itself on as an argument on a PreparedStatement.
  */
trait SQLArgument {
  def set(index: Int, statement: PreparedStatement): Unit
}
