package org.scalarelational.model

import java.util.logging.Level

import com.typesafe.scalalogging.LazyLogging
import org.scalarelational.instruction.InstructionType
import pl.metastack.metarx.Opt

trait SQLLogging extends SQLContainer with LazyLogging {
  val sqlLogLevel = Opt[Level]()

  override protected def calling(instructionType: InstructionType, sql: String): Unit = {
    super.calling(instructionType, sql)
    sqlLogLevel.get match {
      case Some(level) => level match {
        case Level.INFO => logger.info("calling", sql)
        case Level.WARNING => logger.warn("calling", sql)
        case Level.SEVERE => logger.error("calling", sql)
        case _ => throw new RuntimeException(s"Unhandled logging level: $level")
      }
      case None =>  // Don't log
    }
  }
}
