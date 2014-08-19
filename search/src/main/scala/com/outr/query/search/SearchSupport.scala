package com.outr.query.search

import com.outr.query.h2.H2Datastore
import com.outr.query.h2.trigger.{TriggerEvent, TriggerType}
import com.outr.query.Table
import org.powerscala.search.Search
import java.io.File
import org.powerscala.concurrent.Executor

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SearchSupport extends H2Datastore {
  private var searchMap = Map.empty[Table, Search]
  private var subTriggers = Map.empty[Table, Table]

  def processDelay = 0.0
  def delayedCommit = true

  def subTrigger(table: Table, searchableTable: Table) = synchronized {
    subTriggers += table -> searchableTable
  }

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
    case evt if isSearchable(evt.table) => triggeredFor(evt)
    case evt if subTriggers.contains(evt.table) => triggeredFor(evt.copy(table = subTriggers(evt.table)))
    case _ => // Ignore non-searchable table triggers
  }

  private def triggeredFor(evt: TriggerEvent) = if (evt.triggerType.is(TriggerType.Insert, TriggerType.Update)) {
    if (processDelay > 0.0) {
      Executor.schedule(processDelay) {
        updateDocument(evt)
      }
    } else {
      updateDocument(evt)
    }
  } else if (evt.triggerType.is(TriggerType.Delete)) {
    if (processDelay > 0.0) {
      Executor.schedule(processDelay) {
        deleteDocument(evt)
      }
    } else {
      deleteDocument(evt)
    }
  } else {
    // Ignore other types of trigger events
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
