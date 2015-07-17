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

object ColumnPropertyContainer {
  def apply[T](columnProperties: ColumnProperty*)(implicit manifest: Manifest[T]) = new ColumnPropertyContainer {
    override val properties = columnProperties.map(cp => cp.name -> cp).toMap

    override def classType = manifest.runtimeClass
  }
}