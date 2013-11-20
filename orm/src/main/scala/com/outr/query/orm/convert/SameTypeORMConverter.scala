package com.outr.query.orm.convert

import com.outr.query.{QueryResult, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class SameTypeORMConverter[C](column: Column[C]) extends ORMConverter[C, C] {
  def fromORM(column: Column[C], o: C) = Conversion(Some(column(o)), None)

  def toORM(column: Column[C], c: C, result: QueryResult) = Some(c)
}
