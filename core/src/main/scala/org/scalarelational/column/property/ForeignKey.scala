package org.scalarelational.column.property

import org.scalarelational.model.Column

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ForeignKey(fc: => Column[_]) extends ColumnProperty {
  lazy val foreignColumn = fc

  def name = ForeignKey.name
}

object ForeignKey {
  val name = "foreignKey"

  def apply(column: Column[_]) = column.get[ForeignKey](name).getOrElse(throw new RuntimeException(s"No foreign key found on $column"))
}