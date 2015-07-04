package org.scalarelational.result

import java.sql.ResultSet

import org.scalarelational.{SelectExpression, ColumnValue}
import org.scalarelational.fun.{SQLFunctionValue, SQLFunction}
import org.scalarelational.instruction.Query
import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class QueryResultsIterator[E, R](rs: ResultSet, val query: Query[E, R]) extends Iterator[QueryResult[R]] {
  private val NextNotCalled = 0
  private val HasNext = 1
  private val NothingLeft = 2
  private var nextStatus = NextNotCalled

  def converted = new EnhancedIterator[R](this.map(qr => qr.converted))

  def hasNext = synchronized {
    if (nextStatus == NextNotCalled) {
      nextStatus = if (rs.next()) HasNext else NothingLeft
    }
    nextStatus == HasNext
  }

  def nextOption() = if (hasNext) {
    try {
      val values = query.asVector.zipWithIndex.map {
        case (expression, index) => valueFromExpressions[Any](expression.asInstanceOf[SelectExpression[Any]], index)
      }
      Some(QueryResult[R](query.table, values, query.converter))
    } finally {
      synchronized {
        nextStatus = NextNotCalled
      }
    }
  } else {
    None
  }

  def next() = nextOption().getOrElse(throw new RuntimeException("No more results. Use nextOption() instead."))

  protected def valueFromExpressions[T](expression: SelectExpression[T], index: Int) = expression match {
    case column: ColumnLike[_] => {
      val c = column.asInstanceOf[ColumnLike[T]]
      val value = rs.getObject(index + 1)
      val converted =
        if (value == null) null.asInstanceOf[T]
        else c.converter.fromSQLType(c, value)
      ColumnValue[T](c, converted, None)
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

  def head = next()
  def headOption = nextOption()
}

class EnhancedIterator[T](iterator: Iterator[T]) extends Iterator[T] {
  override def hasNext = iterator.hasNext

  override def next() = iterator.next()

  def nextOption() = if (hasNext) Option(next()) else None

  def head = next()
  def headOption = nextOption()

  def one = if (hasNext) {
    val n = next()
    if (hasNext) throw new RuntimeException("More than one result for query!")
    n
  } else {
    throw new RuntimeException("No results for the query!")
  }
}