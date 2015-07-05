package org.scalarelational.model

import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType

/**
 * @author Matt Hicks <matt@outr.com>
 */
private[scalarelational] class Column[T](val name: String,
                                         val converter: DataType[T],
                                         val manifest: Manifest[T],
                                         val table: Table,
                                         val props: Seq[ColumnProperty]
                                        ) extends ColumnLike[T] {
  table.addColumn(this)     // Add this column to the table
  props.foreach(_.addedTo(this))

  val properties = props.map(p => p.name -> p).toMap

  lazy val classType = manifest.runtimeClass
  lazy val longName = s"${table.tableName}.$name"
  lazy val index = table.columns.indexOf(this)
  lazy val fieldName = table.fieldName(this)

  def opt: ColumnLike[Option[T]] = ColumnOption(this)
  def as(alias: String) = ColumnAlias[T](this, None, None, Option(alias))

  def has(property: ColumnProperty): Boolean = has(property.name)
  def has(propertyName: String): Boolean = properties.contains(propertyName)
  def get[P <: ColumnProperty](propertyName: String): Option[P] = properties.get(propertyName).asInstanceOf[Option[P]]
  def prop[P <: ColumnProperty](propertyName: String) = get[P](propertyName)
    .getOrElse(throw new NullPointerException(s"Unable to find property by name '$propertyName' in column '$longName'."))
  def isOptional = classType == classOf[Option[_]]

  override def toString = name
}