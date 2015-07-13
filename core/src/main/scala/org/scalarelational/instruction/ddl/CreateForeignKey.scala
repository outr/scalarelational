package org.scalarelational.instruction.ddl

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CreateForeignKey(tableName: String, columnName: String, foreignTableName: String, foreignColumnName: String)