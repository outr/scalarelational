package org.scalarelational.table.property

import org.scalarelational.Prop


trait TableProperty extends Prop {
  def name: String
}