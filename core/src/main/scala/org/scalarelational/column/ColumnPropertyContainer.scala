package org.scalarelational.column

import org.scalarelational.PropertyContainer
import org.scalarelational.column.property.ColumnProperty

trait ColumnPropertyContainer extends PropertyContainer[ColumnProperty] {
  def optional: Boolean
}

object ColumnPropertyContainer {
  def apply[T](columnProperties: ColumnProperty*)(_optional: Boolean) = new ColumnPropertyContainer {
    override val properties = columnProperties.map(cp => cp.name -> cp).toMap
    override def optional: Boolean = _optional
  }
}