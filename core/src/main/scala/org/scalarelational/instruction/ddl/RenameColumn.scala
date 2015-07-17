package org.scalarelational.instruction.ddl

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class RenameColumn(tableName: String, oldName: String, newName: String)