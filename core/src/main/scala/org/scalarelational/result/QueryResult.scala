package org.scalarelational.result

import org.scalarelational.fun.{SQLFunctionValue, SQLFunction}
import org.scalarelational.{ColumnValue, ExpressionValue}
import org.scalarelational.model.{ColumnLike, Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class QueryResult[Result](table: Table, values: Vector[ExpressionValue[_]], converter: QueryResult[Result] => Result) {
  lazy val converted = converter(this)

  def apply() = converted
  def apply[T](column: Column[T]) = values.collectFirst {
    case cv: ColumnValue[_] if cv.column == column => cv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find column: ${column.name} in result."))

  def apply[T](function: SQLFunction[T]) = values.collectFirst {
    case fv: SQLFunctionValue[_] if function.alias.nonEmpty && fv.function.alias == function.alias => fv.value.asInstanceOf[T]
    case fv: SQLFunctionValue[_] if fv.function == function => fv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find function value: $function in result."))

  def toSimpleMap = {
    values.collect {
      case v if v.expression.isInstanceOf[ColumnLike[_]] => v.expression.asInstanceOf[ColumnLike[_]].name -> v.value
    }.toMap
  }

  def toFieldMap = {
    values.collect {
      case v => {
        val name = v.expression match {
          case c: Column[_] => c.fieldName
          case f: SQLFunction[_] if f.alias.nonEmpty => f.alias.get
          case c: ColumnLike[_] => c.name
        }
        name -> v.value
      }
    }.toMap
  }

  override def toString = s"${table.tableName}(${values.mkString(", ")})"
}