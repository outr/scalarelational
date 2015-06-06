package org.scalarelational.result

import org.scalarelational.fun.{SQLFunctionValue, SQLFunction}
import org.scalarelational.{ColumnValue, ExpressionValue}
import org.scalarelational.model.{ColumnLike, Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class QueryResult(table: Table, values: List[ExpressionValue[_]]) {
  def apply[T](column: Column[T]) = values.collectFirst {
    case cv: ColumnValue[_] if cv.column == column => cv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find column: ${column.name} in result."))

  def apply[T](function: SQLFunction[T]) = values.collectFirst {
    case fv: SQLFunctionValue[_] if fv.function == function => fv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find function value: $function in result."))

  def toSimpleMap = {
    values.collect {
      case v if v.expression.isInstanceOf[ColumnLike[_]] => v.expression.asInstanceOf[ColumnLike[_]].name -> v.value
    }.toMap
  }

  override def toString = s"${table.tableName}: ${values.mkString(", ")}"
}