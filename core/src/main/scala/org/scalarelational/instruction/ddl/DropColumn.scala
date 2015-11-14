package org.scalarelational.instruction.ddl


case class DropColumn(tableName: String, columnName: String, ifExists: Boolean = false)