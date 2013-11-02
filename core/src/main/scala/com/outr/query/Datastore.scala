package com.outr.query

import javax.sql.DataSource

import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Datastore {
  private var _sessions = Map.empty[Thread, DatastoreSession]

  lazy val tables = getClass.fields.collect {
    case f if f.hasType(classOf[Table]) => f[Table](this)
  }

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

  def create(ifNotExist: Boolean = true) = {
    val s = session
    val statement = s.connection.createStatement()
    tables.foreach(t => statement.execute(createTableSQL(ifNotExist, t)))
  }

  def createTableSQL(ifNotExist: Boolean, table: Table): String

  protected def createSession() = new DatastoreSession(this, sessionTimeout, Thread.currentThread())

  protected[query] def cleanup(thread: Thread, session: DatastoreSession) = synchronized {
    _sessions -= thread
  }
}
