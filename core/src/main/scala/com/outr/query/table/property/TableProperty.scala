package com.outr.query.table.property

import com.outr.query.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableProperty {
  def name: String

  def addedTo(table: Table) = {}
}