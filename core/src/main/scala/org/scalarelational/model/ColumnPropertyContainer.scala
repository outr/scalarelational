package org.scalarelational.model

import org.scalarelational.model.property.PropertyContainer
import org.scalarelational.model.property.column.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnPropertyContainer extends PropertyContainer[ColumnProperty] {
  def classType: Class[_]
  def isOptional = classType == classOf[Option[_]]
}

object ColumnPropertyContainer {
  def apply[T](columnProperties: ColumnProperty*)(implicit manifest: Manifest[T]) = new ColumnPropertyContainer {
    override val properties = columnProperties.map(cp => cp.name -> cp).toMap

    override def classType = manifest.runtimeClass
  }
}