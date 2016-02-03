package org.scalarelational

import java.sql.Connection

class Session[D <: Database](database: D) {
  private var _disposed = false
  private var _connection: Option[Connection] = None
  private var _inTransaction: Boolean = false

  def hasConnection: Boolean = _connection.nonEmpty

  def connection: Connection = _connection match {
    case _ if disposed => throw new RuntimeException("Session is disposed.")
    case Some(c) => c
    case None =>
      val c = database.dataSource.getConnection
      _connection = Some(c)
      c
  }

  def autoCommit: Boolean = connection.getAutoCommit
  def autoCommit_=(b: Boolean): Unit = connection.setAutoCommit(b)

  def transactionMode: TransactionMode = TransactionMode.byValue(connection.getTransactionIsolation)
  def transactionMode_=(m: TransactionMode): Unit = connection.setTransactionIsolation(m.value)

  def disposed: Boolean = _disposed
  def dispose(): Unit = if (!disposed) {
    _connection match {
      case Some(c) => c.close()
      case None => // No connection was created
    }
    _disposed = true
  }

  protected[scalarelational] def withTransaction[R](f: => R): R = {
    val alreadyInTransaction = _inTransaction
    try {
      if (!alreadyInTransaction) {
        _inTransaction = true
        connection.setAutoCommit(false)
      }
      f
    } catch {
      case t: Throwable => {
        if (!alreadyInTransaction) {
          connection.rollback()
        }
        throw t
      }
    } finally {
      if (!alreadyInTransaction) {
        _inTransaction = false
        connection.commit()
        connection.setAutoCommit(true)
      }
    }
  }
}