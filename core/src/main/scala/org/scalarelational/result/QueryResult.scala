package org.scalarelational.result

import org.scalarelational.ExpressionValue
import org.scalarelational.column.{Column, ColumnLike, ColumnValue}
import org.scalarelational.fun.{SQLFunction, SQLFunctionValue}
import org.scalarelational.table.Table

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class QueryResult[Result](table: Table, values: Vector[ExpressionValue[_]], converter: QueryResult[Result] => Result) {
  lazy val converted = converter(this)

  def apply() = converted

  def get[T, S](column: Column[T, S]) = values.collectFirst {
    case cv: ColumnValue[_, _] if cv.column == column => cv.value.asInstanceOf[T]
  }

  def apply[T, S](column: Column[T, S]) = {
    get[T, S](column).getOrElse(throw new RuntimeException(s"Unable to find column: ${column.name} in result."))
  }

  def has[T, S](column: Column[T, S]) = {
    val value = get[T, S](column)
    value.nonEmpty && value.get != null && value.get != None
  }

  def apply[T, S](function: SQLFunction[T, S]) = values.collectFirst {
    case fv: SQLFunctionValue[_, _] if function.alias.nonEmpty && fv.function.alias == function.alias => fv.value.asInstanceOf[T]
    case fv: SQLFunctionValue[_, _] if fv.function == function => fv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find function value: $function in result."))

  def toSimpleMap = {
    values.collect {
      case v if v.expression.isInstanceOf[ColumnLike[_, _]] => v.expression.asInstanceOf[ColumnLike[_, _]].name -> v.value
    }.toMap
  }

  def toFieldMap: Map[String, Any] = {
    values.map { v =>
      val name = v.expression match {
        case c: Column[_, _] => c.fieldName
        case f: SQLFunction[_, _] if f.alias.nonEmpty => f.alias.get
        case c: ColumnLike[_, _] => c.name
      }

      name -> v.value
    }.toMap
  }

  def toFieldMapForTable(table: Table) = {
    values.collect {
      case v if v.expression.longName.toLowerCase.startsWith(s"${table.tableName.toLowerCase}.") => {
        val name = v.expression match {
          case c: Column[_, _] => c.fieldName
          case f: SQLFunction[_, _] if f.alias.nonEmpty => f.alias.get
          case c: ColumnLike[_, _] => c.name
        }
        val shortName = if (name.indexOf('.') != -1) {
          name.substring(name.lastIndexOf('.') + 1)
        } else {
          name
        }
        shortName -> v.value
      }
    }.toMap
  }

  override def toString = s"${table.tableName}(${values.mkString(", ")})"
}