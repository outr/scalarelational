package com.outr.query.orm

import com.outr.query.QueryResultsIterator

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMResultsIterator[T](results: QueryResultsIterator, ormt: ORMTable[T]) extends Iterator[T] {
  def hasNext = results.hasNext

  def next() = ormt.result2Instance(results.next())
}
