package com.outr.query.orm

import com.outr.query.QueryResultsIterator

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMResultsIterator[T](results: QueryResultsIterator, table: MappedTable[T]) extends Iterator[T] {
  def hasNext = results.hasNext

  def next() = table.result2Object(results.next())

  def headOption = if (hasNext) {
    Some(next())
  } else {
    None
  }

  def head = headOption.getOrElse(throw new RuntimeException(s"No results found for ORMResultsIterator!"))
}
