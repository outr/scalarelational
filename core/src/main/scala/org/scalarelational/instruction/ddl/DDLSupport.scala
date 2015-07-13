package org.scalarelational.instruction.ddl

import org.scalarelational.CallableInstruction
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.model.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DDLSupport {
  def ddl(table: Table, ifNotExists: Boolean = true): List[CallableInstruction]

  def ddl(create: CreateTable): List[CallableInstruction]

  def ddl[T](create: CreateColumn[T]): List[CallableInstruction]

  def ddl(alter: CreateForeignKey): List[CallableInstruction]

  def ddl(create: CreateIndex): List[CallableInstruction]
}