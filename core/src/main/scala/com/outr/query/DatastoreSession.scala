package com.outr.query

import org.powerscala.concurrent.Temporal

/**
 * @author Matt Hicks <matt@outr.com>
 */
class DatastoreSession private[query](val datastore: Datastore, val timeout: Double, thread: Thread) extends Temporal {
  info("DatastoreSession Created!")
  lazy val connection = {
    info("DatastoreSession connect established!")
    datastore.dataSource.getConnection
  }

  def dispose() = {
    info("DatastoreSession Disposing!")
    connection.close()
    datastore.cleanup(thread, this)
  }
}
