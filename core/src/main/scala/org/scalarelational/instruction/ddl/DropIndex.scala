package org.scalarelational.instruction.ddl


case class DropIndex(tableName: String, indexName: String, ifExists: Boolean = false)