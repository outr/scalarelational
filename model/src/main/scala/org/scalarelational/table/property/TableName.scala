package org.scalarelational.table.property

case class TableName(name: String) extends TableProperty {
  def key: TablePropertyKey[TableName] = TableName
}

object TableName extends TablePropertyKey[TableName]("tableName")