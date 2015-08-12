package org.scalarelational.dsl

import org.scalarelational.column.Column
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.{DataTypeCreator, DataTypeSupport}
import org.scalarelational.instruction.ddl._
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DDLDSLSupport extends DataTypeSupport {
  this: DDLSupport =>

  def createTable(table: Table) = ddl(table2Create(table, ifNotExists = false))
  def createTable(tableName: String) = ddl(CreateTable(tableName))

  def createColumn[T, S](column: Column[T, S]) = ddl(column2Create(column))
  def createColumn[T, S](tableName: String, columnName: String, columnProperty: ColumnProperty*)
                     (implicit manifest: Manifest[T], dataTypeCreator: DataTypeCreator[T, S]) = {
    ddl(CreateColumn[T, S](tableName, columnName, dataTypeCreator.create(), columnProperty.toSeq))
  }

  def renameColumn(tableName: String, oldName: String, newName: String) = {
    ddl(RenameColumn(tableName, oldName, newName))
  }

  def restartColumn(tableName: String, columnName: String, value: Long) = ddl(RestartColumn(tableName, columnName, value))

  def changeColumnType[T, S](tableName: String, columnName: String, properties: ColumnProperty*)
                         (implicit manifest: Manifest[T], dataTypeCreator: DataTypeCreator[T, S]) = {
    ddl(ChangeColumnType[T, S](tableName, columnName, dataTypeCreator.create(), properties: _*)(manifest))
  }

  def dropTable(table: Table, cascade: Boolean) = ddl(DropTable(table.tableName, cascade))
  def dropTable(tableName: String, cascade: Boolean) = ddl(DropTable(tableName, cascade))

  def dropColumn(column: Column[_, _]) = ddl(DropColumn(column.table.tableName, column.name))
  def dropColumn(tableName: String, columnName: String) = ddl(DropColumn(tableName, columnName))

  def dropIndex(indexName: String) = ddl(DropIndex(indexName))
}