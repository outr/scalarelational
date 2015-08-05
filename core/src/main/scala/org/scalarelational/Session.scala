package org.scalarelational

import java.sql.{Connection, Statement}

import org.scalarelational.datatype.DataTyped
import org.scalarelational.model.Datastore

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Session(datastore: Datastore, var inTransaction: Boolean = false) {
  private var _disposed = false
  private var _connection: Option[Connection] = None

  def hasConnection = _connection.nonEmpty
  def connection = _connection match {
    case _ if disposed => throw new RuntimeException("Session is disposed.")
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

  def executeUpdate(sql: String, args: List[DataTyped[_]]) = {
    Datastore.current(datastore)
    val ps = connection.prepareStatement(sql)
    try {
      args.zipWithIndex.foreach {
        case (arg, index) => if (datastore.typesOnQueries) {
          ps.setObject(index + 1, arg.value, arg.dataType.jdbcType)
        } else {
          ps.setObject(index + 1, arg.value)
        }
      }
      ps.executeUpdate()
    } finally {
      ps.close()
    }
  }

  def executeInsert(sql: String, args: Seq[DataTyped[_]]) = {
    Datastore.current(datastore)
    val ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    args.zipWithIndex.foreach {
      case (arg, index) => if (datastore.typesOnQueries) {
        ps.setObject(index + 1, arg.value, arg.dataType.jdbcType)
      } else {
        ps.setObject(index + 1, arg.value)
      }
    }
    ps.executeUpdate()
    ps.getGeneratedKeys
  }

  def executeInsertMultiple(sql: String, rows: Seq[Seq[DataTyped[_]]]) = {
    Datastore.current(datastore)
    val ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    rows.foreach {
      case args => {
        args.zipWithIndex.foreach {
          case (arg, index) => if (datastore.typesOnQueries) {
            ps.setObject(index + 1, arg.value, arg.dataType.jdbcType)
          } else {
            ps.setObject(index + 1, arg.value)
          }
        }
        ps.addBatch()
      }
    }
    ps.executeBatch()
    ps.getGeneratedKeys
  }

  def executeQuery(sql: String, args: Seq[DataTyped[_]]) = {
    Datastore.current(datastore)
    val ps = connection.prepareStatement(sql)
    args.zipWithIndex.foreach {
      case (typed, index) => if (datastore.typesOnQueries) {
        ps.setObject(index + 1, typed.value, typed.dataType.jdbcType)
      } else {
        ps.setObject(index + 1, typed.value)
      }
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

  def disposed = _disposed

  protected[scalarelational] def dispose() = if (!disposed) {
    _connection match {
      case Some(c) => c.close()
      case None => // No connection ever created
    }
    _disposed = true
  }
}