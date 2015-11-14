package org.scalarelational.model

import org.powerscala.log.{Level, Logging}
import org.powerscala.property.Property
import org.scalarelational.instruction.InstructionType


trait SQLLogging extends SQLContainer with Logging {
  val sqlLogLevel = Property[Level](default = None)

  override protected def calling(instructionType: InstructionType, sql: String): Unit = {
    super.calling(instructionType, sql)
    sqlLogLevel.get match {
      case Some(level) => log(level, sql)
      case None => // Don't log
    }
  }
}
