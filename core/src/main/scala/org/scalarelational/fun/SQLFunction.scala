package org.scalarelational.fun

import scala.language.existentials

import org.powerscala.enum.{EnumEntry, Enumerated}

import org.scalarelational.SelectExpression
import org.scalarelational.datatype.DataType
import org.scalarelational.column.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class SQLFunction[T](functionType: FunctionType,
                          column: ColumnLike[_],
                          converter: DataType[T],
                          alias: Option[String] = None) extends SelectExpression[T] {
  override def longName = alias.getOrElse(column.longName)
  def as(alias: String) = copy[T](alias = Some(alias))
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
