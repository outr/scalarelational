package org.scalarelational.column

case class ColumnAlias[T, S](column: ColumnLike[T, S],
                             tableAlias: Option[String],
                             alias: Option[String],
                             as: Option[String]
                            ) extends ColumnLike[T, S] {
  val name = as.getOrElse(columnName)
  val longName = as.fold(s"$tableName.$columnName")(s => s"$tableName.$columnName AS $s")

  def dataType = column.dataType
  def table = column.table

  def tableName = tableAlias.getOrElse(table.tableName)
  def columnName = alias.getOrElse(column.name)

  override def properties = column.properties
}