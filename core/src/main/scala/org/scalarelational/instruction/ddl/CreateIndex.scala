package org.scalarelational.instruction.ddl

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CreateIndex(tableName: String,
                       name: String,
                       columns: List[String] = Nil,
                       unique: Boolean = false,
                       ifNotExists: Boolean = false)