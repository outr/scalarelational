package com.outr.query.orm.convert

import com.outr.query.{QueryResult, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object OptionInt2IntConverter extends ORMConverter[Int, Option[Int]] {
  def fromORM(column: Column[Int], o: Option[Int]) = o match {
    case Some(i) => Conversion(Some(column(i)), None)
    case None => Conversion.empty
  }

  def toORM(column: Column[Int], c: Int, result: QueryResult) = Some(Some(c))
}
