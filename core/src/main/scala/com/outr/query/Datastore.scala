package com.outr.query

import javax.sql.DataSource

import java.sql.ResultSet
import org.powerscala.event.processor.OptionProcessor
import org.powerscala.event.Listenable
import org.powerscala.concurrent.{Time, Executor}
import org.powerscala.log.Logging
import com.outr.query.convert._
import scala.Some

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Datastore extends Listenable with Logging {
  implicit val thisDatastore = this

  private var _sessions = Map.empty[Thread, DatastoreSession]
  val value2SQL = new OptionProcessor[(ColumnLike[_], Any), Any]("value2SQL")
  val sql2Value = new OptionProcessor[(ColumnLike[_], Any), Any]("sql2Value")

  implicit def booleanConverter = BooleanConverter
  implicit def intConverter = IntConverter
  implicit def longConverter = LongConverter
  implicit def doubleConverter = DoubleConverter
  implicit def bigDecimalConverter = BigDecimalConverter
  implicit def stringConverter = StringConverter
  implicit def byteArrayConverter = ByteArrayConverter

  val tables: List[Table]

  private var lastUpdated = System.nanoTime()
  val updater = Executor.scheduleWithFixedDelay(1.0, 1.0) {
    val current = System.nanoTime()
    val delta = Time.fromNanos(current - lastUpdated)
    update(delta)
    lastUpdated = current
  }

  def sessions = _sessions.values
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

  def select(expressions: SelectExpression*) = Query(expressions.toList)
  def select(expressions: List[SelectExpression]) = Query(expressions)
  def insert(values: ColumnValue[_]*) = {
    val results = exec(Insert(values.toList))
    if (results.hasNext) {
      Some(results.next())
    } else {
      None
    }
  }
  def update(values: ColumnValue[_]*) = Update(values.toList, values.head.column.table)
  def delete(table: Table) = Delete(table)

  def dataSource: DataSource
  def sessionTimeout = 5.0

  def create(ifNotExist: Boolean = true) = {
    val s = session
    val statement = s.connection.createStatement()
    val sql = ddl(ifNotExist)
    transaction {
      statement.execute(sql)
    }
  }

  def ddl(ifNotExist: Boolean = true) = {
    val b = new StringBuilder

    tables.foreach {
      case t => {
        b.append(createTableSQL(ifNotExist, t))
        b.append("\r\n")
      }
    }

    tables.foreach {
      case t => {
        b.append(createTableReferences(t))
      }
    }

    b.toString()
  }

  def transactionMode = TransactionMode.byValue(session.connection.getTransactionIsolation)
  def transactionMode_=(mode: TransactionMode) = session.connection.setTransactionIsolation(mode.value)

  private val transactionLayers = new ThreadLocal[Int] {
    override def initialValue() = 0
  }

  /**
   * Creates a transaction for the contents of the supplied function. If an exception is thrown the contents will be
   * rolled back to the savepoint created before the function was invoked. If no exception occurs the transaction
   * will be committed (but only if it is not a nested transaction). Layering of transactions is supported and
   * will defer commit until the last transaction is ended.
   *
   * @param f the function to execute within the transaction
   * @tparam T the return value from the function
   * @return T
   */
  def transaction[T](f: => T) = {
    val layer = transactionLayers.get()
    val isTop = layer == 0                                        // Is this the top-level transaction
    transactionLayers.set(layer + 1)                              // Increment the transaction layer
    active {
      val previousAutoCommit = session.connection.getAutoCommit
      session.connection.setAutoCommit(false)
      val savepointName = s"transaction$layer"
      val savepoint = session.connection.setSavepoint(savepointName)
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
          } else {
            session.connection.rollback(savepoint)
          }
          throw t
        }
      } finally {
        transactionLayers.set(transactionLayers.get() - 1)          // Decrement the transaction layer
        session.connection.setAutoCommit(previousAutoCommit)
        session.connection.releaseSavepoint(savepoint)
      }
    }
  }

  def sqlFromQuery(query: Query): (String, List[Any])

  def exec(query: Query): QueryResultsIterator
  def exec(insert: Insert): Iterator[Int]
  def exec(update: Update): Int
  def exec(delete: Delete): Int

  def createTableSQL(ifNotExist: Boolean, table: Table): String

  def createTableReferences(table: Table): String

  protected def createSession() = new DatastoreSession(this, sessionTimeout, Thread.currentThread())

  protected[query] def cleanup(thread: Thread, session: DatastoreSession) = synchronized {
    _sessions -= thread
  }

  protected def update(delta: Double) = {
    sessions.foreach {
      case session => session.update(delta)
    }
  }
  def active[T](f: => T): T = {
    val s = session
    s.checkIn()
    s.activeQueries.incrementAndGet()
    try {
      f
    } finally {
      s.activeQueries.decrementAndGet()
    }
  }
  def dispose() = {
    sessions.foreach {
      case session => session.dispose()
    }
  }
}

class GeneratedKeysIterator(rs: ResultSet) extends Iterator[Int] {
  def hasNext = rs.next()
  def next() = rs.getInt(1)
}

case class QueryResult(table: Table, values: List[ExpressionValue[_]]) {
  def apply[T](column: Column[T]) = values.collectFirst {
    case cv: ColumnValue[_] if cv.column == column => cv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find column: ${column.name} in result."))

  def apply[T](function: SQLFunction[T]) = values.collectFirst {
    case fv: SQLFunctionValue[_] if fv.function == function => fv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find function value: $function in result."))
}

class QueryResultsIterator(rs: ResultSet, val query: Query) extends Iterator[QueryResult] {
  def hasNext = rs.next()
  def next() = {
    query.table.datastore.session.checkIn()       // Keep the session alive
    val values = query.expressions.zipWithIndex.map {
      case (expression, index) => expression match {
        case column: ColumnLike[_] => ColumnValue[Any](column.asInstanceOf[ColumnLike[Any]], column.converter.fromSQLType(column, rs.getObject(index + 1)), None)
        case function: SQLFunction[_] => SQLFunctionValue[Any](function.asInstanceOf[SQLFunction[Any]], rs.getObject(index + 1))
      }
    }
    QueryResult(query.table, values)
  }
}