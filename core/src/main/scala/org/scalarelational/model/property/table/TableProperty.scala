package org.scalarelational.model.property.table

import org.scalarelational.model.property.Prop

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableProperty extends Prop {
  def name: String
}