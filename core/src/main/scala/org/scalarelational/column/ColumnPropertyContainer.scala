package org.scalarelational.column

import org.scalarelational.PropertyContainer
import org.scalarelational.column.property.ColumnProperty

trait ColumnPropertyContainer extends PropertyContainer[ColumnProperty] {
  def isOptional: Boolean
}

object ColumnPropertyContainer {
  def apply[T](columnProperties: ColumnProperty*)(_isOptional: Boolean) = new ColumnPropertyContainer {
    override val properties = columnProperties.map(cp => cp.name -> cp).toMap
    override def isOptional: Boolean = _isOptional
  }
}