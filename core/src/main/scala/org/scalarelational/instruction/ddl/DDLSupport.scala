package org.scalarelational.instruction.ddl

import org.scalarelational.column.Column
import org.scalarelational.instruction.CallableInstruction
import org.scalarelational.table.Table

import scala.language.implicitConversions


trait DDLSupport {
  def table2Create(table: Table, ifNotExists: Boolean = true): CreateTable
  def column2Create[T, S](column: Column[T, S]): CreateColumn[T, S]

  def ddl(tables: List[Table], ifNotExists: Boolean = true): List[CallableInstruction]

  def ddl(create: CreateTable): List[CallableInstruction]

  def ddl[T, S](create: CreateColumn[T, S]): List[CallableInstruction]

  def ddl(alter: CreateForeignKey): List[CallableInstruction]

  def ddl(create: CreateIndex): List[CallableInstruction]

  def ddl(alter: RenameColumn): List[CallableInstruction]

  def ddl(alter: RestartColumn): List[CallableInstruction]

  def ddl[T, S](alter: ChangeColumnType[T, S]): List[CallableInstruction]

  def ddl(drop: DropTable): List[CallableInstruction]

  def ddl(drop: DropColumn): List[CallableInstruction]

  def ddl(drop: DropIndex): List[CallableInstruction]
}