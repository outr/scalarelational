package com.outr.query.orm.convert

import com.outr.query.{QueryResult, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ORMConverter[C, O] {
  def fromORM(column: Column[C], o: O): Conversion[C, O]

  def toORM(column: Column[C], c: C, result: QueryResult): Option[O]
}
