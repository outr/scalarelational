package org.scalarelational.instruction.ddl


case class RestartColumn(tableName: String, columnName: String, value: Long)