package org.scalarelational.instruction.ddl


case class CreateIndex(tableName: String,
                       name: String,
                       columns: List[String] = Nil,
                       unique: Boolean = false,
                       ifNotExists: Boolean = false)