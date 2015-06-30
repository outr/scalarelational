package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Table(val tableName: String) extends ModelEntry {
  override def toSQL = SQL(tableName)
}