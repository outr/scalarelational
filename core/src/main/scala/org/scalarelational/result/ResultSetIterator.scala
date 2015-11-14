package org.scalarelational.result

import java.sql.ResultSet


class ResultSetIterator(results: ResultSet) extends Iterator[ResultSet] {
  def hasNext: Boolean = results.next()
  def next(): ResultSet = results
}