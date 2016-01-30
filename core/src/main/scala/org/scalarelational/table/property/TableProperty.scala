package org.scalarelational.table.property

trait TableProperty {
  def key: TablePropertyKey
}

class TablePropertyKey(val key: String)