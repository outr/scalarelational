package org.scalarelational.table.property

import org.scalarelational.Prop

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableProperty extends Prop {
  def name: String
}