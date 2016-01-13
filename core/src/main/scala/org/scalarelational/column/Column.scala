package org.scalarelational.column

import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType
import org.scalarelational.table.Table

class Column[T, S] private[scalarelational](val name: String,
                                            val dataType: DataType[T, S],
                                            val table: Table,
                                            val props: Seq[ColumnProperty]
                                           ) extends ColumnLike[T, S] {
  this.props(props: _*)

  lazy val longName = s"${table.tableName}.$name"
  lazy val index = table.columns.indexOf(this)
  lazy val fieldName = table.fieldName(this)

  def as(alias: String): ColumnAlias[T, S] = ColumnAlias[T, S](this, None, None, Option(alias))

  override def toString: String = name
}