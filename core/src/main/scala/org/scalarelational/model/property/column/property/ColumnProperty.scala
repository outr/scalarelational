package org.scalarelational.model.property.column.property

import org.scalarelational.model.property.Prop

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnProperty extends Prop {
  def name: String
}