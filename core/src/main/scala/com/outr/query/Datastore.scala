package com.outr.query

import javax.sql.DataSource

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Datastore {
  private var _sessions = Map.empty[Thread, DatastoreSession]

  def session = synchronized {
    _sessions.get(Thread.currentThread()) match {
      case Some(s) => s
      case None => {
        val s = createSession()
        _sessions += Thread.currentThread() -> s
        s
      }
    }
  }

  def dataSource: DataSource
  def sessionTimeout = 5.0

  def createTableSQL(ifNotExist: Boolean, table: Table): String

  protected def createSession() = new DatastoreSession(this, sessionTimeout, Thread.currentThread())

  protected[query] def cleanup(thread: Thread, session: DatastoreSession) = synchronized {
    _sessions -= thread
  }
}
