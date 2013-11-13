package com.outr.query

import org.powerscala.reflect._
import com.outr.query.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Column[T] private(val name: String)(implicit val manifest: Manifest[T], val table: Table) extends ColumnLike[T] {
  private var _properties = Map.empty[String, ColumnProperty]
  def properties = _properties.values

  lazy val classType: EnhancedClass = manifest.runtimeClass

  lazy val longName = s"${table.tableName}.$name"

  table.addColumn(this)     // Add this column to the table

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
  def prop[P <: ColumnProperty](propertyName: String) = _properties.get(propertyName).asInstanceOf[Option[P]]

  override def toString = s"Column(${table.tableName}.$name)"
}

object Column {
  def apply[T](name: String, properties: ColumnProperty*)(implicit manifest: Manifest[T], table: Table) = {
    val c = new Column[T](name)
    c.props(properties: _*)
  }
}