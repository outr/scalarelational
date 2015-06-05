package com.outr.query

import java.sql.{Statement, Connection}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Session(datastore: Datastore, var inTransaction: Boolean = false) {
  private var _connection: Option[Connection] = None

  def connection = _connection match {
    case Some(c) => c
    case None => {
      val c = datastore.dataSource.getConnection
      _connection = Option(c)
      c
    }
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

  def commit() = connection.commit()
  def rollback() = connection.rollback()

  protected[query] def dispose() = {
    _connection match {
      case Some(c) => c.close()
      case None => // No connection ever created
    }
  }
}