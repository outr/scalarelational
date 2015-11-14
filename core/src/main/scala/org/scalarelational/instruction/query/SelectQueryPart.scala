package org.scalarelational.instruction.query

import org.scalarelational.instruction.Query
import org.scalarelational.result.QueryResult
import org.scalarelational.table.Table

case class SelectQueryPart[Types, Result](expressions: SelectExpressions[Types],
                                          converter: QueryResult => Result) {
  def from(table: Table) = Query[Types, Result](expressions, table, converter = converter)
}