package org.scalarelational.instruction.ddl

import org.scalarelational.CallableInstruction
import org.scalarelational.column.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DDLSupport {
  def ddl(create: CreateTable): List[CallableInstruction]

  def ddl[T](create: CreateColumn[T]): List[CallableInstruction]
}