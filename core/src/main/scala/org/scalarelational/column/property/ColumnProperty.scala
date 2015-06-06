package org.scalarelational.column.property

import org.scalarelational.Column

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnProperty {
  def name: String

  def addedTo(column: Column[_]) = {}
}