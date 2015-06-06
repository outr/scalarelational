package com.outr.query

import javax.sql.DataSource

import java.sql.ResultSet
import org.powerscala.event.processor.OptionProcessor
import org.powerscala.event.Listenable
import org.powerscala.log.Logging

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Datastore extends Listenable with Logging with SessionSupport {
  implicit def thisDatastore: Datastore = this

  val value2SQL = new OptionProcessor[(ColumnLike[_], Any), Any]("value2SQL")
  val sql2Value = new OptionProcessor[(ColumnLike[_], Any), Any]("sql2Value")

  private var _tables = Map.empty[String, Table]
  protected[query] def add(table: Table) = synchronized {
    _tables += table.tableName.toLowerCase -> table
  }
  def tableByName(name: String) = _tables.get(name.toLowerCase)

  /**
   * Called when the datastore is being created for the first time. This does not mean the tables are being created but
   * just the datastore.
   */
  def creating(): Unit = {}

  def dataSource: DataSource

  def jdbcTables = {
    val s = session
    val meta = s.connection.getMetaData
    val results = meta.getTables(null, "PUBLIC", "%", null)
    try {
      new ResultSetIterator(results).map(_.getString("TABLE_NAME")).toSet
    } finally {
      results.close()
    }
  }

  def empty() = jdbcTables.isEmpty

  def create(tables: Table*) = {
    if (tables.isEmpty) throw new RuntimeException(s"Datastore.create must include all tables that need to be created.")
    val s = session
    val sql = ddl(tables: _*)
    s.execute(sql)
  }

  def ddl(tables: Table*) = {
    if (tables.isEmpty) throw new RuntimeException(s"Datastore.ddl must include all tables that need to be generated.")
    val b = new StringBuilder

    val existingTables = jdbcTables

    tables.foreach {
      case t => if (!existingTables.contains(t.tableName.toUpperCase)) {
        b.append(createTableSQL(t))
        b.append("\r\n")
      } else {
        debug(s"Table already exists: ${t.tableName}")
      }
    }

    tables.foreach {
      case t => if (!existingTables.contains(t.tableName.toUpperCase)) {
        createTableExtras(t, b)
      }
    }

    createExtras(b)

    b.toString()
  }

  def sqlFromQuery(query: Query): (String, List[Any])

  def exec(query: Query): QueryResultsIterator
  def exec(insert: InsertSingle): Int
  def exec(insert: InsertMultiple): List[Int]
  def exec(merge: Merge): Int
  def exec(update: Update): Int
  def exec(delete: Delete): Int

  def createTableSQL(table: Table): String

  def createTableExtras(table: Table, b: StringBuilder): Unit

  def createExtras(b: StringBuilder): Unit

  def dispose() = {}
}

object Datastore {
  private val instance = new ThreadLocal[Datastore]

  protected[query] def current(d: Datastore) = instance.set(d)
  def apply() = instance.get()
}

case class QueryResult(table: Table, values: List[ExpressionValue[_]]) {
  def apply[T](column: Column[T]) = values.collectFirst {
    case cv: ColumnValue[_] if cv.column == column => cv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find column: ${column.name} in result."))

  def apply[T](function: SQLFunction[T]) = values.collectFirst {
    case fv: SQLFunctionValue[_] if fv.function == function => fv.value.asInstanceOf[T]
  }.getOrElse(throw new RuntimeException(s"Unable to find function value: $function in result."))

  override def toString = s"${table.tableName}: ${values.mkString(", ")}"
}

class QueryResultsIterator(rs: ResultSet, val query: Query) extends Iterator[QueryResult] {
  def hasNext = rs.next()
  def next() = {
    val values = query.expressions.zipWithIndex.map {
      case (expression, index) => expression match {
        case column: ColumnLike[_] => ColumnValue[Any](column.asInstanceOf[ColumnLike[Any]], column.converter.fromSQLType(column, rs.getObject(index + 1)), None)
        case function: SQLFunction[_] => SQLFunctionValue[Any](function.asInstanceOf[SQLFunction[Any]], rs.getObject(index + 1))
      }
    }
    QueryResult(query.table, values)
  }

  def one = if (hasNext) {
    val n = next()
    if (hasNext) throw new RuntimeException("More than one result for query!")
    n
  } else {
    throw new RuntimeException("No results for the query!")
  }
}

class ResultSetIterator(results: ResultSet) extends Iterator[ResultSet] {
  def hasNext = results.next()
  def next() = results
}