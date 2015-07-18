package org.scalarelational.model

import org.scalarelational.model.property.table.TableProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TablePropertyContainer {
  def properties: Map[String, TableProperty]

  final def has(property: TableProperty): Boolean = has(property.name)
  final def has(propertyName: String): Boolean = properties.contains(propertyName)
  final def get[P <: TableProperty](propertyName: String) = properties.get(propertyName).asInstanceOf[Option[P]]
  final def prop[P <: TableProperty](propertyName: String) = get[P](propertyName).getOrElse(throw new NullPointerException(s"Unable to find property by name '$propertyName' in ${getClass.getSimpleName} $this."))
}