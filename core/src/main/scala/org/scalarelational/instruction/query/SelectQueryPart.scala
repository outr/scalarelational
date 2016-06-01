package org.scalarelational.instruction.query

import org.scalarelational.Session
import org.scalarelational.instruction.Query
import org.scalarelational.result.QueryResult
import org.scalarelational.table.Table

case class SelectQueryPart[Types, Result](expressions: SelectExpressions[Types],
                                          converter: QueryResult => Result) {
  def from(table: Table): Query[Types, Result] = Query[Types, Result](expressions, table.datastore, Some(table), converter = converter)

  /** TODO Should SelectQueryPart inherit from Query? This would allow to directly
    * call `result` and `converted` without creating an intermediary Query object.
    */
  def query(implicit session: Session): Query[Types, Result] =
    Query[Types, Result](expressions, session.datastore, None, converter = converter)
}