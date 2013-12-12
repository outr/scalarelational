package com.outr.query.orm.convert

import com.outr.query.{QueryResult, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Option2ValueConverter[T] extends ORMConverter[T, Option[T]] {
  def fromORM(column: Column[T], o: Option[T]) = o match {
    case Some(i) => Conversion(Some(column(i)), None)
    case None => Conversion.empty
  }

  def toORM(column: Column[T], c: T, result: QueryResult) = Some(Some(c))
}
