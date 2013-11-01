package com.outr.query

import org.powerscala.concurrent.Temporal

/**
 * @author Matt Hicks <matt@outr.com>
 */
class DatastoreSession private[query](val datastore: Datastore, val timeout: Double, thread: Thread) extends Temporal {
  lazy val connection = datastore.dataSource.getConnection

  def dispose() = {
    connection.close()
    datastore.cleanup(thread, this)
  }
}
