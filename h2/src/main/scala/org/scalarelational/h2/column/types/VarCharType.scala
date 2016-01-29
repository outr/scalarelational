package org.scalarelational.h2.column.types

import org.scalarelational.column.types.ColumnType

case class VarCharType(columnName: Option[String] = None,
                       isUnique: Boolean = false,
                       isIgnoreCase: Boolean = false) extends ColumnType[String] {
  def unique: VarCharType = copy(isUnique = true)
  def ignoreCase: VarCharType = copy(isIgnoreCase = true)
}

object VarCharType extends VarCharType(None, false, false)