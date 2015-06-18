package org.scalarelational.result

import java.sql.ResultSet

import org.scalarelational.{SelectExpression, ColumnValue}
import org.scalarelational.fun.{SQLFunctionValue, SQLFunction}
import org.scalarelational.instruction.Query
import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class QueryResultsIterator[E, R](rs: ResultSet, val query: Query[E, R]) extends Iterator[R] {
  def hasNext = rs.next()
  def next() = {
    val values = query.expressionsList.zipWithIndex.map {
      case (expression, index) => valueFromExpressions[Any](expression.asInstanceOf[SelectExpression[Any]], index)
    }
    query.converter(QueryResult(query.table, values))
  }

  protected def valueFromExpressions[T](expression: SelectExpression[T], index: Int) = expression match {
    case column: ColumnLike[_] => {
      val c = column.asInstanceOf[ColumnLike[T]]
      ColumnValue[T](c, c.converter.fromSQLType(c, rs.getObject(index + 1)), None)
    }
    case function: SQLFunction[_] => {
      val f = function.asInstanceOf[SQLFunction[T]]
      SQLFunctionValue[T](f, rs.getObject(index + 1).asInstanceOf[T])
    }
  }

  def one = if (hasNext) {
    val n = next()
    if (hasNext) throw new RuntimeException("More than one result for query!")
    n
  } else {
    throw new RuntimeException("No results for the query!")
  }

  def head = {
    if (!hasNext) throw new RuntimeException(s"No items available.")
    next()
  }
  def headOption = if (hasNext) {
    Some(next())
  } else {
    None
  }
}