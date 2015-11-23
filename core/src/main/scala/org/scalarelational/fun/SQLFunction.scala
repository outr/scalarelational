package org.scalarelational.fun

import org.scalarelational.SelectExpression
import org.scalarelational.column.ColumnLike
import org.scalarelational.datatype.{DataType, DataTypes}

import scala.language.existentials


case class SQLFunction[T, S](functionType: FunctionType,
                          column: ColumnLike[_, _],
                          converter: DataType[T, S],
                          alias: Option[String] = None) extends SelectExpression[T] {
  override def longName: String = alias.getOrElse(column.longName)
  def as(alias: String): SQLFunction[T, S] = copy[T, S](alias = Some(alias))
}

trait FunctionType {
  def sql: String
}

case class SpecificFunctionType[T, S](sql: String, converter: DataType[T, S]) extends FunctionType {
  def apply(column: ColumnLike[_, _]): SQLFunction[T, S] = {
    SQLFunction[T, S](this, column, converter)
  }
}

case class DerivedFunctionType(sql: String) extends FunctionType {
  def apply[T, S](column: ColumnLike[T, S]): SQLFunction[T, S] = {
    SQLFunction[T, S](this, column, column.dataType)
  }
}

trait BasicFunctionTypes {
  val Avg = DerivedFunctionType("AVG")
  val BoolAnd = SpecificFunctionType("BOOL_AND", DataTypes.BooleanType)
  val BoolOr = SpecificFunctionType("BOOL_OR", DataTypes.BooleanType)
  val Count = SpecificFunctionType("COUNT", DataTypes.LongType)
  val GroupConcat = SpecificFunctionType("GROUP_CONCAT", DataTypes.StringType)
  val Max = DerivedFunctionType("MAX")
  val Min = DerivedFunctionType("MIN")
  val Sum = DerivedFunctionType("SUM")
}
