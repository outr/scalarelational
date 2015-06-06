package org.scalarelational.result

import java.sql.ResultSet

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ResultSetIterator(results: ResultSet) extends Iterator[ResultSet] {
  def hasNext = results.next()
  def next() = results
}