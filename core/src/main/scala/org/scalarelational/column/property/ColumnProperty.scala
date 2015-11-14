package org.scalarelational.column.property

import org.scalarelational.Prop


trait ColumnProperty extends Prop {
  def name: String
}