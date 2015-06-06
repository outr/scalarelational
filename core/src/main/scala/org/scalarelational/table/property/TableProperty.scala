package org.scalarelational.table.property

import org.scalarelational.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableProperty {
  def name: String

  def addedTo(table: Table) = {}
}