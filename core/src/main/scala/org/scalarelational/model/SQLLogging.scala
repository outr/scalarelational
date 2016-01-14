package org.scalarelational.model

import org.powerscala.log.{Level, Logging}
import org.scalarelational.instruction.InstructionType
import pl.metastack.metarx.Opt

trait SQLLogging extends SQLContainer with Logging {
  val sqlLogLevel = Opt[Level]()

  override protected def calling(instructionType: InstructionType, sql: String): Unit = {
    super.calling(instructionType, sql)
    sqlLogLevel.get match {
      case Some(level) => log(level, sql)
      case None =>  // Don't log
    }
  }
}
