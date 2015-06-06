package org.scalarelational

import org.powerscala.enum.{EnumEntry, Enumerated}

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLFunction[T] extends SelectExpression {
  def functionType: FunctionType
}

sealed abstract class FunctionType(val sql: String) extends EnumEntry

object FunctionType extends Enumerated[FunctionType] {
  case object Avg extends FunctionType("AVG")
  case object BoolAnd extends FunctionType("BOOL_AND")
  case object BoolOr extends FunctionType("BOOL_OR")
  case object Count extends FunctionType("COUNT")
  case object GroupConcat extends FunctionType("GROUP_CONCAT")
  case object Max extends FunctionType("MAX")
  case object Min extends FunctionType("MIN")
  case object Sum extends FunctionType("SUM")

  val values = findValues.toVector
}

case class SimpleFunction[T](functionType: FunctionType, column: ColumnLike[_]) extends SQLFunction[T]