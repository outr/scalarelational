package org.scalarelational.table

import org.scalarelational.column.Column
import org.scalarelational.column.property.{PrimaryKey, AutoIncrement, ForeignKey}
import org.scalarelational.model.Datastore
import org.scalarelational.table.property.{Index, Linking}

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