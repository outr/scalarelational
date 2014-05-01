package com.outr.query.orm

import com.outr.query.{QueryResult, QueryResultsIterator}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMResultsIterator[T](results: QueryResultsIterator, result2Object: QueryResult => T) extends Iterator[T] {
  def hasNext = results.hasNext

  def next() = result2Object(results.next())

  def headOption = if (hasNext) {
    Some(next())
  } else {
    None
  }

  def head = headOption.getOrElse(throw new RuntimeException(s"No results found for ORMResultsIterator!"))
}
