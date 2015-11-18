package org.scalarelational.instruction

import java.sql.PreparedStatement

import org.powerscala.reflect._
import org.scalarelational.Session
import org.scalarelational.model.Datastore

import scala.util.matching.Regex

case class SQLInstruction[T <: AnyRef](sql: String, clazz: EnhancedClass) {
  lazy val (preparedSQL, builder) = prepared()

  private def prepared(): (String, (PreparedStatement, T) => Unit) = {
    var builders = List.empty[(PreparedStatement, T) => Unit]
    var index = 0
    val prepared = SQLInstruction.ArgumentRegex.replaceAllIn(sql, (m: Regex.Match) => {
      val argName = m.group(1)
      val caseValue = clazz.caseValue(argName).getOrElse(throw new RuntimeException(s"Unable to find case value for $argName."))
      index += 1
      val builder = (ps: PreparedStatement, instance: T) => {
        ps.setObject(index, caseValue[AnyRef](instance))
      }
      builders = builder :: builders
      "?"
    })

    val reversed = builders.reverse
    val b = (ps: PreparedStatement, instance: T) => reversed.foreach(f => f(ps, instance))
    (prepared, b)
  }
}

object SQLInstruction {
  val ArgumentRegex = """\$(\S+)""".r

  def apply[T <: AnyRef](sql: String)(implicit manifest: Manifest[T]) = new SQLInstruction[T](sql, manifest.runtimeClass)
}

case class SQLInstructionInstance[T <: AnyRef](instruction: SQLInstruction[T],
                                               datastore: Datastore) {
  def preparedStatement(session: Session) =
    session.connection.prepareStatement(instruction.preparedSQL)

  def execute(instances: T*)(implicit session: Session) = {
    var first = true
    val stmt = preparedStatement(session)
    instances.foreach {
      case instance => {
        if (first) {
          first = false
        } else {
          stmt.addBatch()
        }
        instruction.builder(stmt, instance)
      }
    }
    stmt.execute()
  }
}