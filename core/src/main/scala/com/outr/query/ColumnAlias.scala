package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnAlias[T](column: ColumnLike[T], tableAlias: String, alias: String) extends ColumnLike[T] {
  val name = alias
  val longName = s"$tableAlias.$name"
  def converter = column.converter
  val table = column.table
  def manifest = column.manifest
}