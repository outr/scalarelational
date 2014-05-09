package com.outr.query.orm.convert

import java.sql.Timestamp
import com.outr.query.{QueryResult, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Timestamp2Long extends ORMConverter[Timestamp, Long] {
  override def fromORM(column: Column[Timestamp], o: Long) = Conversion(Some(column(new Timestamp(o))), None)

  override def toORM(column: Column[Timestamp], c: Timestamp, result: QueryResult) = c match {
    case null => None
    case _ => Some(c.getTime)
  }
}
