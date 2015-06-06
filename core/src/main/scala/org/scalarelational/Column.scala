package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Column[T] private[scalarelational](val name: String,
                        val converter: DataType[T],
                        val manifest: Manifest[T],
                        val table: Table) extends ColumnLike[T] {
  private var _properties = Map.empty[String, ColumnProperty]
  def properties = _properties.values

  lazy val classType: EnhancedClass = manifest.runtimeClass

  lazy val longName = s"${table.tableName}.$name"

  lazy val index = table.columns.indexOf(this)

  table.addColumn(this)     // Add this column to the table

  /**
   * Adds the supplied properties to this column.
   *
   * @param properties the properties to add
   * @return this
   */
  def props(properties: ColumnProperty*) = synchronized {
    properties.foreach {
      case p => {
        _properties += p.name -> p
        p.addedTo(this)
      }
    }
    this
  }

  def as(alias: String) = ColumnAlias[T](this, table.tableName, alias)

  def has(property: ColumnProperty): Boolean = has(property.name)
  def has(propertyName: String): Boolean = _properties.contains(propertyName)
  def get[P <: ColumnProperty](propertyName: String) = _properties.get(propertyName).asInstanceOf[Option[P]]
  def prop[P <: ColumnProperty](propertyName: String) = get[P](propertyName).getOrElse(throw new NullPointerException(s"Unable to find property by name '$propertyName' in column '$longName'."))

  override def toString = s"Column(${table.tableName}.$name)"
}