package org.scalarelational.column.property

import org.scalarelational.Prop

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnProperty extends Prop {
  def name: String
}