package org.scalarelational.dsl

import org.scalarelational.instruction.ddl.{DropColumn, DDLSupport, DropTable}
import org.scalarelational.model.{Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DDLDSLSupport {
  this: DDLSupport =>

  def dropTable(table: Table) = ddl(DropTable(table.tableName))
  def dropTable(tableName: String) = ddl(DropTable(tableName))

  def dropColumn(column: Column[_]) = ddl(DropColumn(column.table.tableName, column.name))
  def dropColumn(tableName: String, columnName: String) = ddl(DropColumn(tableName, columnName))
}