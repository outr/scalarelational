package org.scalarelational.model

import org.scalarelational.model.property.column.property.{PrimaryKey, ForeignKey, AutoIncrement}
import org.scalarelational.model.property.table.{Index, Linking}

/**
 * LinkingTable provides a quick and convenience setup for creating a standard linking table.
 *
 * @author Matt Hicks <matt@outr.com>
 */
class LinkingTable(name: String,
                   leftColumn: Column[Option[Int]],
                   rightColumn: Column[Option[Int]])
                  (implicit datastore: Datastore) extends Table(name, Linking)(datastore) {
  val left = column[Int](s"${leftColumn.table.tableName}Id", new ForeignKey(leftColumn))
  val right = column[Int](s"${rightColumn.table.tableName}Id", new ForeignKey(rightColumn))
  val id = column[Option[Int]]("id", AutoIncrement, PrimaryKey)

  props(Index.unique(s"unique${left.name}${right.name}", left, right))
}