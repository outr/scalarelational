package com.outr.query

import org.powerscala.concurrent.{AtomicInt, Temporal}
import org.powerscala.MapStorage
import java.sql.{Statement, Savepoint}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class DatastoreSession private[query](val datastore: Datastore, val timeout: Double, thread: Thread) extends Temporal {
  private[query] val activeQueries = new AtomicInt(0)
  /**
   * Allows storage of key/value pairs on this session that will be removed upon disposal
   */
  val store = new MapStorage[Any, Any]()
  @volatile private var connectionCreated = false

  lazy val connection = {
    val c = datastore.dataSource.getConnection
    connectionCreated = true
    Datastore.current(datastore)
    c
  }

  def execute(sql: String) = {
    Datastore.current(datastore)
    val statement = connection.createStatement()
    try {
      statement.execute(sql)
    } finally {
      statement.close()
    }
  }

  def executeUpdate(sql: String, args: List[Any]) = {
    Datastore.current(datastore)
    val ps = connection.prepareStatement(sql)
    try {
      args.zipWithIndex.foreach {
        case (value, index) => ps.setObject(index + 1, value)
      }
      ps.executeUpdate()
    } finally {
      ps.close()
    }
  }

  def executeInsert(sql: String, args: List[Any]) = {
    Datastore.current(datastore)
    val ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    args.zipWithIndex.foreach {
      case (value, index) => ps.setObject(index + 1, value)
    }
    ps.executeUpdate()
    ps.getGeneratedKeys
  }

  def executeQuery(sql: String, args: List[Any]) = {
    Datastore.current(datastore)
    val ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    args.zipWithIndex.foreach {
      case (value, index) => ps.setObject(index + 1, value)
    }
    ps.executeQuery()
  }

  def autoCommit = connection.getAutoCommit
  def autoCommit(b: Boolean) = connection.setAutoCommit(b)
  def transactionMode = TransactionMode.byValue(connection.getTransactionIsolation)
  def transactionMode_=(mode: TransactionMode) = connection.setTransactionIsolation(mode.value)
  def savePoint(name: String) = connection.setSavepoint(name)
  def releaseSavePoint(savePoint: Savepoint) = connection.releaseSavepoint(savePoint)

  def commit() = connection.commit()
  def rollback(savePoint: Savepoint = null) = if (savePoint != null) {
    connection.rollback(savePoint)
  } else {
    connection.rollback()
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
    if (connectionCreated) {
      connection.close()
    }
    datastore.cleanup(thread, this)
  }
}