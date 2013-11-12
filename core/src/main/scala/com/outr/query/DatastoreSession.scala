package com.outr.query

import org.powerscala.concurrent.{AtomicInt, Temporal}
import org.powerscala.MapStorage

/**
 * @author Matt Hicks <matt@outr.com>
 */
class DatastoreSession private[query](val datastore: Datastore, val timeout: Double, thread: Thread) extends Temporal {
  private[query] val activeQueries = new AtomicInt(0)
  /**
   * Allows storage of key/value pairs on this session that will be removed upon disposal
   */
  val store = new MapStorage[Any, Any]()

  lazy val connection = {
    datastore.dataSource.getConnection
  }

  override def checkIn() = super.checkIn()

  override def update(delta: Double) = {
    if (activeQueries() > 0) {
      checkIn()       // Check in if there is an active query
    }
    super.update(delta)
  }

  def dispose() = {
    store.clear()
    connection.close()
    datastore.cleanup(thread, this)
  }
}
