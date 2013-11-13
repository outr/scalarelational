package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLFunction[T] extends SelectExpression {
  def functionType: FunctionType
}

class FunctionType private(val sql: String) extends EnumEntry

object FunctionType extends Enumerated[FunctionType] {
  val Avg = new FunctionType("AVG")
  val BoolAnd = new FunctionType("BOOL_AND")
  val BoolOr = new FunctionType("BOOL_OR")
  val Count = new FunctionType("COUNT")
  val GroupConcat = new FunctionType("GROUP_CONCAT")
  val Max = new FunctionType("MAX")
  val Min = new FunctionType("MIN")
  val Sum = new FunctionType("SUM")
}

case class SimpleFunction[T](functionType: FunctionType, column: ColumnLike[_]) extends SQLFunction[T]