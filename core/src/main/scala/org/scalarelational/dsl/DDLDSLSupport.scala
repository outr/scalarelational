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

  def dropColumn(column: Column, ifExists: Boolean = false) = ddl(DropColumn(column.table.tableName, column.name, ifExists))
  def dropColumn(tableName: String, columnName: String, ifExists: Boolean = false) = ddl(DropColumn(tableName, columnName, ifExists))
}