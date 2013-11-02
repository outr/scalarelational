package com.outr.query

import javax.sql.DataSource

import org.powerscala.reflect._
import java.sql.ResultSet
import scala.collection.immutable.ListMap

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

  def select(columns: Column[_]*) = Query(columns.toList)
  def select(columns: List[Column[_]]) = Query(columns)
  def insert(values: ColumnValue[_]*) = {
    var map = ListMap[Column[_], ColumnValue[_]](values.map(cv => cv.column -> cv): _*)
    val table = values.head.column.table
    table.columns.foreach {
      case c => {
        if (c.default.nonEmpty && !map.contains(c)) {
          map += c -> ColumnValue[Any](c.asInstanceOf[Column[Any]], c.default.get)
        }
      }
    }
    exec(Insert(map.values.toList))
  }

  def dataSource: DataSource
  def sessionTimeout = 5.0

  def create(ifNotExist: Boolean = true) = {
    val s = session
    val statement = s.connection.createStatement()
    tables.foreach(t => statement.execute(createTableSQL(ifNotExist, t)))
  }

  def transactionMode = TransactionMode.byValue(session.connection.getTransactionIsolation)
  def transactionMode_=(mode: TransactionMode) = session.connection.setTransactionIsolation(mode.value)

  private val transactionLayers = new ThreadLocal[Int] {
    override def initialValue() = 0
  }

  /**
   * Creates a transaction for the contents of the supplied function. If an exception is thrown the contents will be
   * rolled back. If no exception occurs the transaction will be committed. Layering of transactions is supported and
   * will defer commit until the last transaction is ended.
   *
   * Note: even an exception thrown by a inner transaction will not be rolled back, the exception must be thrown all
   * the way to the top-level transaction for the rollback to occur.
   *
   * @param f the function to execute within the transaction
   * @tparam T the return value from the function
   * @return T
   */
  def transaction[T](f: => T) = {
    val isTop = transactionLayers.get() == 0                      // Is this the top-level transaction
    transactionLayers.set(transactionLayers.get() + 1)            // Increment the transaction layer
    val previousAutoCommit = session.connection.getAutoCommit
    session.connection.setAutoCommit(false)
    try {
      val result = f
      if (isTop) {
        session.connection.commit()
      }
      result
    } catch {
      case t: Throwable => {
        if (isTop) {
          session.connection.rollback()
        }
        throw t
      }
    } finally {
      transactionLayers.set(transactionLayers.get() - 1)          // Decrement the transaction layer
      session.connection.setAutoCommit(previousAutoCommit)
    }
  }

  def exec(query: Query): QueryResultsIterator
  def exec(insert: Insert): Iterator[Long]

  def createTableSQL(ifNotExist: Boolean, table: Table): String

  protected def createSession() = new DatastoreSession(this, sessionTimeout, Thread.currentThread())

  protected[query] def cleanup(thread: Thread, session: DatastoreSession) = synchronized {
    _sessions -= thread
  }
}

class GeneratedKeysIterator(rs: ResultSet) extends Iterator[Long] {
  def hasNext = rs.next()
  def next() = rs.getLong(1)
}

case class QueryResult(table: Table, values: List[ColumnValue[_]]) {
  def apply[T](column: Column[T]) = values.find(cv => cv.column == column).getOrElse(throw new RuntimeException(s"Unable to find column: ${column.name} in result.")).value.asInstanceOf[T]
}

class QueryResultsIterator(rs: ResultSet, query: Query) extends Iterator[QueryResult] {
  def hasNext = rs.next()
  def next() = {
    val values = query.columns.zipWithIndex.map {
      case (column, index) => ColumnValue[Any](column.asInstanceOf[Column[Any]], rs.getObject(index + 1))
    }
    QueryResult(query.table, values)
  }
}