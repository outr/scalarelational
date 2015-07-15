package org.scalarelational.instruction

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait InstructionType extends EnumEntry

object InstructionType extends Enumerated[InstructionType] {
  case object DDL extends InstructionType
  case object Delete extends InstructionType
  case object Insert extends InstructionType
  case object Merge extends InstructionType
  case object Query extends InstructionType
  case object Update extends InstructionType

  val values = findValues.toVector
}