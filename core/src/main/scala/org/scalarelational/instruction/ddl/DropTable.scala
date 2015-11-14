package org.scalarelational.instruction.ddl


case class DropTable(tableName: String, cascade: Boolean = false)