package org.scalarelational.instruction

import java.sql.{PreparedStatement, SQLException}

import org.scalarelational.{Database, Session}

/**
  * SQL Instruction that can be invoked and return a response.
  */
trait Instruction[D <: Database, +R] {
  def describe: String
  def args: Vector[Any]

  def exec()(implicit session: Session[D]): R

  protected def call(session: Session[D]): Boolean = {
    val ps = session.connection.prepareStatement(describe)
    args.zipWithIndex.foreach {
      case (arg, index) => set(index + 1, arg, ps)
    }
    try {
      ps.execute()
    } catch {
      case t: Throwable => throw new SQLException(s"Exception thrown while executing SQL call: $describe with args: ${args.mkString(", ")}.", t)
    } finally {
      ps.close()
    }
  }

  protected def set(index: Int, arg: Any, ps: PreparedStatement): Unit = arg match {
    case i: Int => ps.setInt(index, i)
    case s: String => ps.setString(index, s)
    case _ => throw new UnsupportedOperationException(s"Attempting to set an unsupported type (${arg.getClass}) to a prepared statement. Value: $arg.")
  }
}