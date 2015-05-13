package com.outr.query.search

import com.outr.query.table.property.TableProperty
import com.outr.query.Table
import com.outr.query.h2.Triggers
import com.outr.query.h2.trigger.TriggerEvent
import org.apache.lucene.index.Term
import org.powerscala.search.{DocumentUpdate, Search}
import com.outr.query.orm.ORMTable
import org.powerscala.log.Logging

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Searchable extends TableProperty {
  def name = Searchable.name

  def updateDocument(evt: TriggerEvent): Option[DocumentUpdate]

  def deleteDocument(evt: TriggerEvent): Either[Option[DocumentUpdate], Term]

  override def addedTo(table: Table) = {
    super.addedTo(table)

    if (!table.has(Triggers.name)) {    // Make sure triggers are enabled on the table
      table.props(Triggers.Normal)
    }
  }
}

trait BasicSearchable extends Searchable {
  def event2DocumentUpdate(evt: TriggerEvent): Option[DocumentUpdate]

  override def updateDocument(evt: TriggerEvent) = event2DocumentUpdate(evt)

  override def deleteDocument(evt: TriggerEvent) = Left(event2DocumentUpdate(evt))
}

trait ORMSearchable[T] extends Searchable with Logging {
  def table: ORMTable[T]

  def updateById(id: Any, table: ORMTable[T]) = table.byId(id) match {
    case Some(t) => toDocumentUpdate(t)
    case None => {
      warn(s"Unable to find instance in ${table.tableName} by id: $id.")
      None
    }
  }

  def toDocumentUpdate(t: T): Option[DocumentUpdate]

  def update(t: T) = toDocumentUpdate(t) match {
    case Some(update) => {
      info(s"Table: $table")
      info(s"Datastore: ${table.datastore}")
      val datastore = table.datastore.asInstanceOf[SearchSupport]
      datastore.update(datastore.searchForTable(table), update)
    }
    case None => // Ignore
  }

  override def updateDocument(evt: TriggerEvent) = {
    val table = evt.table.asInstanceOf[ORMTable[T]]
    val idColumn = table.primaryKeys.head
    val id = evt(idColumn)
    updateById(id, table)
  }
}

object Searchable {
  val name = "searchable"
}