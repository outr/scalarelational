package com.outr.query.column.property

import com.outr.query.Column

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ForeignKey(val foreignColumn: Column[_]) extends ColumnProperty {
  def name = ForeignKey.name

  override def addedTo(column: Column[_]) = {
    super.addedTo(column)

    foreignColumn.table.addForeignColumn(column)
  }
}

object ForeignKey {
  val name = "foreignKey"

  def apply(column: Column[_]) = column.get[ForeignKey](name).getOrElse(throw new RuntimeException(s"No foreign key found on $column"))
}