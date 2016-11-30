package org.scalarelational.fun

import org.scalarelational.SelectExpression
import org.scalarelational.column.ColumnLike
import org.scalarelational.datatype.{DataType, DataTypes}

import scala.language.existentials

case class SQLFunction[T, S](functionType: FunctionType,
                             columns: Seq[ColumnLike[_, _]],
                             converter: DataType[T, S],
                             alias: Option[String] = None) extends SelectExpression[T] {
  def as(alias: String): SQLFunction[T, S] = copy[T, S](alias = Some(alias))

  override def toSQL: String = {
    val columns = this.columns.map(_.toSQL).mkString(", ")
    alias match {
      case Some(a) => s"${functionType.sql}($columns) AS $a"
      case None => s"${functionType.sql}($columns)"
    }
  }
}

trait FunctionType {
  def sql: String
}

/** SQL function without arguments */
case class DefaultFunctionType[T, S](sql: String, converter: DataType[T, S]) extends FunctionType {
  def apply(): SQLFunction[T, S] = {
    SQLFunction[T, S](this, Seq.empty, converter)
  }
}

case class SpecificFunctionType[T, S](sql: String, converter: DataType[T, S]) extends FunctionType {
  def apply(column: ColumnLike[_, _]): SQLFunction[T, S] = {
    SQLFunction[T, S](this, Seq(column), converter)
  }
}

case class DerivedFunctionType(sql: String) extends FunctionType {
  def apply[T, S](column: ColumnLike[T, S]): SQLFunction[T, S] = {
    SQLFunction[T, S](this, Seq(column), column.dataType)
  }
}

/** SQL function with several arguments */
case class ValueFunctionType[T, S](sql: String, converter: DataType[T, S]) extends FunctionType {
  // TODO Should also take values
  def apply(columns: ColumnLike[T, S]*): SQLFunction[T, S] = {
    SQLFunction[T, S](this, columns, converter)
  }
}

trait BasicFunctionTypes {
  val Avg = DerivedFunctionType("AVG")
  val BoolAnd = SpecificFunctionType("BOOL_AND", DataTypes.BooleanType)
  val BoolOr = SpecificFunctionType("BOOL_OR", DataTypes.BooleanType)
  val Concat = ValueFunctionType("CONCAT", DataTypes.StringType)
  val Count = SpecificFunctionType("COUNT", DataTypes.LongType)
  val GroupConcat = SpecificFunctionType("GROUP_CONCAT", DataTypes.StringType)
  val Max = DerivedFunctionType("MAX")
  val Min = DerivedFunctionType("MIN")
  /** @note Not available in H2 */
  val Now = DefaultFunctionType("NOW", DataTypes.TimestampType)
  val Sum = DerivedFunctionType("SUM")
  /** @note Not available in H2 */
  val UnixTimestamp = DefaultFunctionType("UNIX_TIMESTAMP", DataTypes.LongType)
}
