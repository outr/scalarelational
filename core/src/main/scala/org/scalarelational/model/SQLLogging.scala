package org.scalarelational.model

import com.outr.reactify.Var
import scribe.{Level, Logging}
import org.scalarelational.instruction.InstructionType

trait SQLLogging extends SQLContainer with Logging {
  val sqlLogLevel: Var[Option[Level]] = Var(None)

  override protected def calling(instructionType: InstructionType, sql: String): Unit = {
    super.calling(instructionType, sql)

    sqlLogLevel.get.foreach(logger.log(_, sql, "org.scalarelational.model.SQLLogging"))
  }
}
