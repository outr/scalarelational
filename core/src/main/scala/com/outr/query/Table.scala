package com.outr.query

import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class Table(val tableName: String) {
  implicit def thisTable = this

  lazy val columns = getClass.fields.collect {
    case f if f.hasType(classOf[Column[_]]) => f[Column[_]](this)
  }

  def * = columns
}