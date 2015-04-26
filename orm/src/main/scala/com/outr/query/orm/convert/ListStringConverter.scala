package com.outr.query.orm.convert

import com.outr.query.{QueryResult, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object ListStringConverter extends ORMConverter[String, List[String]] {
  override def fromORM(column: Column[String], o: List[String]): Conversion[String, List[String]] = {
    Conversion(Some(column(o.mkString("|"))))
  }

  override def toORM(column: Column[String], c: String, result: QueryResult): Option[List[String]] = c match {
    case "" | null => Some(Nil)
    case _ => Some(c.split('|').toList)
  }
}