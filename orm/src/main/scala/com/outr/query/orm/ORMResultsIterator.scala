package com.outr.query.orm

import com.outr.query.QueryResultsIterator

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMResultsIterator[T](results: QueryResultsIterator, orm: ORMTable[T]) extends Iterator[T] {
  def hasNext = results.hasNext

  def next() = orm.result2Instance(results.next())

  def headOption = if (hasNext) {
    Some(next())
  } else {
    None
  }

  def head = headOption.getOrElse(throw new RuntimeException(s"No results found for ORMResultsIterator!"))
}
