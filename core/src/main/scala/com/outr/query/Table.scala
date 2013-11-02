package com.outr.query

import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class Table(val tableName: String) {
  implicit def thisTable = this

  lazy val columns: List[Column[_]] = getClass.fields.collect {
    case f if f.hasType(classOf[Column[_]]) => f[Column[_]](this)
  }
  private lazy val columnMap = Map(columns.map(c => c.name -> c): _*)

  def * = columns

  def column[T](name: String) = columnMap.get(name).asInstanceOf[Option[Column[T]]]
}