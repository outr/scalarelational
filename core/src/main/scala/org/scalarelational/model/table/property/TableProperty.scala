package org.scalarelational.model.table.property

import org.scalarelational.model.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableProperty {
  def name: String

  def addedTo(table: Table) = {}
}