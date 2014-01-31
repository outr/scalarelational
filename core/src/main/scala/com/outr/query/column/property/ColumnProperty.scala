package com.outr.query.property

import com.outr.query.Column

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnProperty {
  def name: String

  def addedTo(column: Column[_]) = {}
}