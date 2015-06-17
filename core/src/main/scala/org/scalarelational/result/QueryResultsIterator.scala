package org.scalarelational.result

import java.sql.ResultSet

import org.scalarelational.ColumnValue
import org.scalarelational.fun.{SQLFunctionValue, SQLFunction}
import org.scalarelational.instruction.Query
import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class QueryResultsIterator[R](rs: ResultSet, val query: Query[R]) extends Iterator[R] {
  def hasNext = rs.next()
  def next() = {
    val values = query.expressions.zipWithIndex.map {
      case (expression, index) => expression match {
        case column: ColumnLike[_] => ColumnValue[Any](column.asInstanceOf[ColumnLike[Any]], column.converter.fromSQLType(column, rs.getObject(index + 1)), None)
        case function: SQLFunction[_] => SQLFunctionValue[Any](function.asInstanceOf[SQLFunction[Any]], rs.getObject(index + 1))
      }
    }
    query.converter(QueryResult(query.table, values))
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