package org.scalarelational.model

import com.outr.scribe.{Level, Logging}
import org.scalarelational.instruction.InstructionType
import pl.metastack.metarx.Opt

trait SQLLogging extends SQLContainer with Logging {
  val sqlLogLevel = Opt[Level]()

  override protected def calling(instructionType: InstructionType, sql: String): Unit = {
    super.calling(instructionType, sql)

    sqlLogLevel.get.foreach(logger.log(_, sql))
  }
}
