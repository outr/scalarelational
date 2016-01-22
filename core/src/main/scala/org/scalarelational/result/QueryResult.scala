package org.scalarelational.result

import org.scalarelational.ExpressionValue
import org.scalarelational.column.{Column, ColumnLike, ColumnValue}
import org.scalarelational.fun.{SQLFunction, SQLFunctionValue}
import org.scalarelational.table.Table

import scala.language.existentials

case class QueryResult(table: Table, values: Vector[ExpressionValue[_]]) {
  def getByName[T](name: String): Option[T] = values.collectFirst {
    case cv: ColumnValue[_, _] if cv.column.name.equalsIgnoreCase(name) => cv.value.asInstanceOf[T]
  }

  def byName[T](name: String): T = {
    getByName[T](name).getOrElse(throw new NullPointerException(s"Unable to find value by name: $name for $toSimpleMap"))
  }

  def get[T, S](column: ColumnLike[T, S]): Option[T] = values.collectFirst {
    case cv: ColumnValue[_, _] if cv.column == column => Option(cv.value.asInstanceOf[T])
  }.flatten

  def apply[T, S](column: ColumnLike[T, S]): T = {
    get[T, S](column).getOrElse(throw new RuntimeException(s"Unable to find column: ${column.name} in result."))
  }

  def has[T, S](column: ColumnLike[T, S]): Boolean = {
    val value = get[T, S](column)
    value.nonEmpty && value.get != None
  }

  def apply[T, S](function: SQLFunction[T, S]): T = values.collectFirst {
    case fv: SQLFunctionValue[_, _] if function.alias.nonEmpty && fv.function.alias == function.alias => fv.value.asInstanceOf[T]
    case fv: SQLFunctionValue[_, _] if fv.function == function => fv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find function value: $function in result."))

  def toSimpleMap: Map[String, Any] = {
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

  def toFieldMapForTable(table: Table): Map[String, Any] = {
    values.collect {
      case v if v.expression.longName.toLowerCase.startsWith(s"${table.tableName.toLowerCase}.") => {
        val name = v.expression match {
          case c: Column[_, _] => c.fieldName
          case f: SQLFunction[_, _] if f.alias.nonEmpty => f.alias.get
          case c: ColumnLike[_, _] => c.name
        }

        val shortName =
          name.lastIndexOf('.') match {
            case -1 => name
            case i  => name.substring(i + 1)
          }

        shortName -> v.value
      }
    }.toMap
  }

  override def toString: String = s"${table.tableName}(${values.mkString(", ")})"
}