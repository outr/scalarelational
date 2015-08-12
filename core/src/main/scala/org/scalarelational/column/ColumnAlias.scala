package org.scalarelational.column

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnAlias[T, S](column: ColumnLike[T, S],
                          tableAlias: Option[String],
                          alias: Option[String],
                          as: Option[String]
                         ) extends ColumnLike[T, S] {
  val name = columnName
  val longName = as match {
    case Some(s) => s"$tableName.$columnName AS [$s]"
    case None => s"$tableName.$columnName"
  }
  def dataType = column.dataType
  def table = column.table
  def manifest = column.manifest

  def tableName = tableAlias.getOrElse(table.tableName)
  def columnName = alias.getOrElse(column.name)

  override def classType = column.classType

  override def properties = column.properties
}