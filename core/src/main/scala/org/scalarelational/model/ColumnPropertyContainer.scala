package org.scalarelational.model

import org.scalarelational.column.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnPropertyContainer {
  def classType: Class[_]
  def properties: Map[String, ColumnProperty]

  def has(property: ColumnProperty): Boolean = has(property.name)
  def has(propertyName: String): Boolean = properties.contains(propertyName)
  def get[P <: ColumnProperty](propertyName: String): Option[P] = properties.get(propertyName).asInstanceOf[Option[P]]
  def prop[P <: ColumnProperty](propertyName: String) = get[P](propertyName).getOrElse(throw new NullPointerException(s"Unable to find property by name '$propertyName' in $this."))
  def isOptional = classType == classOf[Option[_]]
}
