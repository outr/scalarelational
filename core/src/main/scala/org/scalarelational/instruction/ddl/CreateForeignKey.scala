package org.scalarelational.instruction.ddl


case class CreateForeignKey(tableName: String, columnName: String, foreignTableName: String, foreignColumnName: String)