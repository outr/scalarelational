package org.scalarelational.dsl

import org.scalarelational.column.Column
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.{DataType, DataTypeSupport, SimpleDataType}
import org.scalarelational.instruction.CallableInstruction
import org.scalarelational.instruction.ddl._
import org.scalarelational.table.Table

trait DDLDSLSupport extends DataTypeSupport {
  this: DDLSupport =>

  def createTable(table: Table): List[CallableInstruction] = ddl(table2Create(table, ifNotExists = false))
  def createTable(tableName: String): List[CallableInstruction] = ddl(CreateTable(tableName))

  def createColumn[T, S](column: Column[T, S]): List[CallableInstruction] = ddl(column2Create(column))
  def createColumn[T, S](tableName: String, columnName: String, columnProperty: ColumnProperty*)
                     (implicit dataType: DataType[T, S]): List[CallableInstruction] =
    ddl(CreateColumn[T, S](tableName, columnName, dataType, columnProperty.toSeq))
  def createColumn[T](tableName: String, columnName: String, columnProperty: ColumnProperty*)
                     (implicit dataType: SimpleDataType[T]): List[CallableInstruction] =
    ddl(CreateColumn[T, T](tableName, columnName, dataType, columnProperty.toSeq))

  def renameColumn(tableName: String, oldName: String, newName: String): List[CallableInstruction] =
    ddl(RenameColumn(tableName, oldName, newName))

  def restartColumn(tableName: String, columnName: String, value: Long): List[CallableInstruction] =
    ddl(RestartColumn(tableName, columnName, value))

  def changeColumnType[T, S](tableName: String, columnName: String, properties: ColumnProperty*)
                         (implicit dataType: DataType[T, S]): List[CallableInstruction] =
    ddl(ChangeColumnType[T, S](tableName, columnName, dataType, properties: _*))

  def dropTable(table: Table, cascade: Boolean): List[CallableInstruction] = ddl(DropTable(table.tableName, cascade))
  def dropTable(tableName: String, cascade: Boolean): List[CallableInstruction] = ddl(DropTable(tableName, cascade))

  def dropColumn(column: Column[_, _]): List[CallableInstruction] = ddl(DropColumn(column.table.tableName, column.name))
  def dropColumn(tableName: String, columnName: String): List[CallableInstruction] = ddl(DropColumn(tableName, columnName))

  def dropIndex(indexName: String): List[CallableInstruction] = ddl(DropIndex(indexName))
}