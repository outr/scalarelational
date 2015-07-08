package org.scalarelational.model

import org.scalarelational.column.property.ColumnProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnAlias[T](column: ColumnLike[T],
                          tableAlias: Option[String],
                          alias: Option[String],
                          as: Option[String]
                         ) extends ColumnLike[T] {
  val name = columnName
  val longName = as match {
    case Some(s) => s"$tableName.$columnName AS [$s]"
    case None => s"$tableName.$columnName"
  }
  def converter = column.converter
  def table = column.table
  def manifest = column.manifest

  def tableName = tableAlias.getOrElse(table.tableName)
  def columnName = alias.getOrElse(column.name)

  def has(property: ColumnProperty): Boolean = column.has(property)
  def get[P <: ColumnProperty](propertyName: String): Option[P] = column.get(propertyName)

  def isOptional: Boolean = column.isOptional
}