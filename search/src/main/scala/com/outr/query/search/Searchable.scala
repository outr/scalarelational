package com.outr.query.search

import com.outr.query.table.property.TableProperty
import com.outr.query.Table
import com.outr.query.h2.Triggers
import com.outr.query.h2.trigger.TriggerEvent
import org.powerscala.search.{DocumentUpdate, Search}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Searchable extends TableProperty {
  def name = Searchable.name

  def updateDocument(search: Search, evt: TriggerEvent): Unit

  def deleteDocument(search: Search, evt: TriggerEvent): Unit

  override def addedTo(table: Table) = {
    super.addedTo(table)

    if (!table.has(Triggers.name)) {    // Make sure triggers are enabled on the table
      table.props(Triggers.Normal)
    }
  }
}

trait BasicSearchable extends Searchable {
  def event2DocumentUpdate(evt: TriggerEvent): DocumentUpdate

  override def updateDocument(search: Search, evt: TriggerEvent) = search.update(event2DocumentUpdate(evt))

  override def deleteDocument(search: Search, evt: TriggerEvent) = search.delete(event2DocumentUpdate(evt))
}

object Searchable {
  val name = "searchable"
}