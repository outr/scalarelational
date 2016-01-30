package org.scalarelational.table.property

case class TableName(name: String) extends TableProperty {
  def key: TablePropertyKey = TableName
}

object TableName extends TablePropertyKey("tableName")