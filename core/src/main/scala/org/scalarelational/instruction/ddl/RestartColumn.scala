package org.scalarelational.instruction.ddl

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class RestartColumn(tableName: String, columnName: String, value: Long)