package org.scalarelational.model.property.table

import org.scalarelational.model.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableProperty {
  def name: String

  def addedTo(table: Table) = {}
}