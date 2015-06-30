package org.scalarelational.model2.query

import org.scalarelational.model2._

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query[G <: Group[Field[_]]](fields: G, table: Table) extends ModelEntry {
  override def toSQL = {
    val fieldsSQL = fields.items.map(_.toSQL)
    val tableSQL = table.toSQL

    val b = new StringBuilder
    b.append("SELECT(")
    b.append(fieldsSQL.map(_.text).mkString(", "))
    b.append(") FROM ")
    b.append(tableSQL.text)

    SQL(b.toString(), fieldsSQL.map(_.args) ::: List(tableSQL.args))
  }
}