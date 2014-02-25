package com.outr.query.search

import com.outr.query.h2.H2Datastore
import com.outr.query.h2.trigger.{TriggerEvent, TriggerType}
import com.outr.query.Table
import org.powerscala.search.Search
import java.io.File

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SearchSupport extends H2Datastore {
  private var searchMap = Map.empty[Table, Search]

  def delayedCommit = true

  final def searchForTable(table: Table) = synchronized {
    searchMap.get(table) match {
      case Some(search) => search
      case None => {
        val search = createSearchForTable(table)
        searchMap += table -> search
        search
      }
    }
  }

  protected def createSearchForTable(table: Table): Search

  def directory: Option[File] = None

  trigger.on {
    case evt if isSearchable(evt.table) => if (evt.triggerType.is(TriggerType.Insert, TriggerType.Update)) {
      updateDocument(evt)
    } else if (evt.triggerType.is(TriggerType.Delete)) {
      deleteDocument(evt)
    } else {
      // Ignore other types of trigger events
    }
    case _ => // Ignore non-searchable table triggers
  }

  private def updateDocument(evt: TriggerEvent) = {
    val search = searchForTable(evt.table)
    searchable(evt.table).updateDocument(search, evt)
    if (delayedCommit) {
      search.requestCommit()
    } else {
      search.commit()
    }
  }

  private def deleteDocument(evt: TriggerEvent) = {
    val search = searchForTable(evt.table)
    searchable(evt.table).deleteDocument(search, evt)
    if (delayedCommit) {
      search.requestCommit()
    } else {
      search.commit()
    }
  }

  private def searchable(table: Table) = table.get[Searchable](Searchable.name).getOrElse(throw new NullPointerException(s"No Searchable property for ${table.tableName}."))

  private def isSearchable(table: Table) = table.has(Searchable.name)
}