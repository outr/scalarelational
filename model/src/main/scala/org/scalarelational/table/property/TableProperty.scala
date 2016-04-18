package org.scalarelational.table.property

trait TableProperty {
  def key: TablePropertyKey[_ <: TableProperty]
}

class TablePropertyKey[Prop <: TableProperty](val key: String)