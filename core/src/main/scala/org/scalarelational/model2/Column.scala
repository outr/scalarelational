package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Column[V](table: Table, name: String) extends Field[V] {
  override def toSQL = SQL(s"${table.tableName}.$name")
}