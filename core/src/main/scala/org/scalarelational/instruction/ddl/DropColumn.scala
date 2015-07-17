package org.scalarelational.instruction.ddl

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class DropColumn(tableName: String, columnName: String, ifExists: Boolean = false)