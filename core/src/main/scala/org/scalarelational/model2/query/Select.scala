package org.scalarelational.model2.query

import org.scalarelational.model2.{Table, Field, Group}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Select[G <: Group[Field[_]]](val fields: G) {
  def from(table: Table) = Query(fields, table)
}