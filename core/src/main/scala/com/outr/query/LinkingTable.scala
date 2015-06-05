package com.outr.query

import com.outr.query.column.property.{ForeignKey, NotNull, PrimaryKey, AutoIncrement}
import com.outr.query.table.property.{Index, Linking}

/**
 * LinkingTable provides a quick and convenience setup for creating a standard linking table.
 *
 * @author Matt Hicks <matt@outr.com>
 */
class LinkingTable(name: String, leftColumn: Column[Int], rightColumn: Column[Int])(implicit datastore: Datastore) extends Table(name, Linking)(datastore) {
  val id = column[Int]("id", AutoIncrement, PrimaryKey)
  val left = column[Int](s"${leftColumn.table.tableName}Id", NotNull, new ForeignKey(leftColumn))
  val right = column[Int](s"${rightColumn.table.tableName}Id", NotNull, new ForeignKey(rightColumn))

  props(Index.unique(s"unique${left.name}${right.name}", left, right))
}