package org.scalarelational.result

import java.sql.ResultSet

import org.scalarelational.ColumnValue
import org.scalarelational.fun.{SQLFunctionValue, SQLFunction}
import org.scalarelational.instruction.Query
import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class QueryResultsIterator(rs: ResultSet, val query: Query) extends Iterator[QueryResult] {
  def hasNext = rs.next()
  def next() = {
    val values = query.expressions.zipWithIndex.map {
      case (expression, index) => expression match {
        case column: ColumnLike[_] => ColumnValue[Any](column.asInstanceOf[ColumnLike[Any]], column.converter.fromSQLType(column, rs.getObject(index + 1)), None)
        case function: SQLFunction[_] => SQLFunctionValue[Any](function.asInstanceOf[SQLFunction[Any]], rs.getObject(index + 1))
      }
    }
    QueryResult(query.table, values)
  }

  def one = if (hasNext) {
    val n = next()
    if (hasNext) throw new RuntimeException("More than one result for query!")
    n
  } else {
    throw new RuntimeException("No results for the query!")
  }
}