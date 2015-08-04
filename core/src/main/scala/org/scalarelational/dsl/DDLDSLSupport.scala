package org.scalarelational.dsl

import org.scalarelational.table.Table
import org.scalarelational.column.Column
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.instruction.ddl._
import org.scalarelational.datatype.{DataTypes, DataType}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DDLDSLSupport extends DataTypes {
  this: DDLSupport =>

  def createTable(table: Table) = ddl(table2Create(table, ifNotExists = false))
  def createTable(tableName: String) = ddl(CreateTable(tableName))

  def createColumn[T](column: Column[T]) = ddl(column2Create(column))
  def createColumn[T](tableName: String, columnName: String, columnProperty: ColumnProperty*)
                     (implicit manifest: Manifest[T], dataType: DataType[T]) = {
    ddl(CreateColumn[T](tableName, columnName, dataType, columnProperty.toSeq))
  }

  def renameColumn(tableName: String, oldName: String, newName: String) = {
    ddl(RenameColumn(tableName, oldName, newName))
  }

  def restartColumn(tableName: String, columnName: String, value: Long) = ddl(RestartColumn(tableName, columnName, value))

  def changeColumnType[T](tableName: String, columnName: String, properties: ColumnProperty*)
                         (implicit manifest: Manifest[T], dataType: DataType[T]) = {
    ddl(ChangeColumnType[T](tableName, columnName, dataType, properties: _*)(manifest))
  }

  def dropTable(table: Table, cascade: Boolean) = ddl(DropTable(table.tableName, cascade))
  def dropTable(tableName: String, cascade: Boolean) = ddl(DropTable(tableName, cascade))

  def dropColumn(column: Column[_]) = ddl(DropColumn(column.table.tableName, column.name))
  def dropColumn(tableName: String, columnName: String) = ddl(DropColumn(tableName, columnName))

  def dropIndex(indexName: String) = ddl(DropIndex(indexName))
}