package org.scalarelational.dsl

import org.scalarelational.instruction.ddl.{DDLSupport, DropTable}
import org.scalarelational.model.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DDLDSLSupport {
  this: DDLSupport =>

  def dropTable(table: Table) = ddl(DropTable(table.tableName))
  def dropTable(tableName: String) = ddl(DropTable(tableName))
}