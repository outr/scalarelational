package org.scalarelational.instruction.ddl


case class RenameColumn(tableName: String, oldName: String, newName: String)