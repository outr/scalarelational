package org.scalarelational.model

import java.sql.ResultSet

import javax.sql.DataSource

import org.powerscala.event.Listenable
import org.powerscala.event.processor.OptionProcessor
import org.powerscala.log.Logging

import org.scalarelational.column.ColumnLike
import org.scalarelational.dsl.{DDLDSLSupport, DSLSupport}
import org.scalarelational.instruction._
import org.scalarelational.instruction.ddl.DDLSupport
import org.scalarelational.result.ResultSetIterator
import org.scalarelational.table.Table
import org.scalarelational.SessionSupport

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Datastore extends Listenable with Logging with SessionSupport with DSLSupport with SQLContainer with DDLSupport with DDLDSLSupport {
  implicit def thisDatastore: Datastore = this

  def DefaultVarCharLength = 65535
  def DefaultBinaryLength  = 1000

  val value2SQL = new OptionProcessor[(ColumnLike[_], Any), Any]("value2SQL")
  val sql2Value = new OptionProcessor[(ColumnLike[_], Any), Any]("sql2Value")

  private var _tables = Map.empty[String, Table]
  protected[scalarelational] def add(table: Table) = synchronized {
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

  def jdbcColumns(tableName: String) = {
    val s = session
    val meta = s.connection.getMetaData
    val results = meta.getColumns(null, "PUBLIC", tableName, null)
    try {
      new ResultSetIterator(results).map(_.getString("COLUMN_NAME")).toSet
    } finally {
      results.close()
    }
  }

  def doesTableExist(name: String) = {
    val s = session
    val meta = s.connection.getMetaData
    val results = meta.getTables(null, "PUBLIC", name.toUpperCase, null)
    try {
      results.next()
    } finally {
      results.close()
    }
  }

  def empty() = jdbcTables.isEmpty

  def create(tables: Table*) = {
    if (tables.isEmpty) throw new RuntimeException(s"Datastore.create must include all tables that need to be created.")
    val sql = ddl(tables.toList)
    sql.result
  }

  def describe[E, R](query: Query[E, R]): (String, List[Any])

  private[scalarelational] final def exec[E, R](query: Query[E, R]): ResultSet = {
    val table = query.table
    val q = SQLContainer.beforeInvoke(table, query)
    try {
      invoke[E, R](q)
    } finally {
      SQLContainer.afterInvoke(table, q)
    }
  }
  private[scalarelational] final def exec(insert: InsertSingle): Int = {
    val table = insert.table
    val i = SQLContainer.beforeInvoke(table, insert)
    try {
      invoke(i)
    } finally {
      SQLContainer.afterInvoke(table, i)
    }
  }
  private[scalarelational] final def exec(insert: InsertMultiple): List[Int] = {
    val table = insert.table
    val i = SQLContainer.beforeInvoke(table, insert)
    try {
      invoke(i)
    } finally {
      SQLContainer.afterInvoke(table, i)
    }
  }
  private[scalarelational] final def exec(merge: Merge): Int = {
    val table = merge.table
    val m = SQLContainer.beforeInvoke(table, merge)
    try {
      invoke(m)
    } finally {
      SQLContainer.afterInvoke(table, m)
    }
  }
  private[scalarelational] final def exec(update: Update): Int = {
    val table = update.table
    val u = SQLContainer.beforeInvoke(table, update)
    try {
      invoke(u)
    } finally {
      SQLContainer.afterInvoke(table, u)
    }
  }
  private[scalarelational] final def exec(delete: Delete): Int = {
    val table = delete.table
    val d = SQLContainer.beforeInvoke(table, delete)
    try {
      invoke(d)
    } finally {
      SQLContainer.afterInvoke(table, d)
    }
  }

  protected def invoke[E, R](query: Query[E, R]): ResultSet
  protected def invoke(insert: InsertSingle): Int
  protected def invoke(insert: InsertMultiple): List[Int]
  protected def invoke(merge: Merge): Int
  protected def invoke(update: Update): Int
  protected def invoke(delete: Delete): Int

  def dispose() = {}

  implicit class CallableInstructions(instructions: List[CallableInstruction]) {
    def result = {
      instructions.foreach(i => i.execute(thisDatastore))
      instructions.size
    }
    def async = thisDatastore.async(result)
    def and(moreInstructions: List[CallableInstruction]) = new CallableInstructions(instructions ::: moreInstructions)
  }
}

object Datastore {
  private val instance = new ThreadLocal[Datastore]

  protected[scalarelational] def current(d: Datastore) = instance.set(d)
  def apply() = instance.get()
}