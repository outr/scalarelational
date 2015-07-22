package org.scalarelational.model

import org.scalarelational.instruction._
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLContainer {
  protected def beforeInvoke[E, R](query: Query[E, R]): Query[E, R] = query
  protected def beforeInvoke(insert: InsertSingle): InsertSingle = insert
  protected def beforeInvoke(insert: InsertMultiple): InsertMultiple = insert
  protected def beforeInvoke(merge: Merge): Merge = merge
  protected def beforeInvoke(update: Update): Update = update
  protected def beforeInvoke(delete: Delete): Delete = delete
  protected def calling(instructionType: InstructionType, sql: String): Unit = {}
  protected def afterInvoke[E, R](query: Query[E, R]): Unit = {}
  protected def afterInvoke(insert: InsertSingle): Unit = {}
  protected def afterInvoke(insert: InsertMultiple): Unit = {}
  protected def afterInvoke(merge: Merge): Unit = {}
  protected def afterInvoke(update: Update): Unit = {}
  protected def afterInvoke(delete: Delete): Unit = {}
}

object SQLContainer {
  def beforeInvoke[E, R](table: Table, query: Query[E, R]): Query[E, R] = table.datastore.beforeInvoke[E, R](table.beforeInvoke[E, R](query))
  def beforeInvoke(table: Table, insert: InsertSingle): InsertSingle = table.datastore.beforeInvoke(table.beforeInvoke(insert))
  def beforeInvoke(table: Table, insert: InsertMultiple): InsertMultiple = table.datastore.beforeInvoke(table.beforeInvoke(insert))
  def beforeInvoke(table: Table, merge: Merge): Merge = table.datastore.beforeInvoke(table.beforeInvoke(merge))
  def beforeInvoke(table: Table, update: Update): Update = table.datastore.beforeInvoke(table.beforeInvoke(update))
  def beforeInvoke(table: Table, delete: Delete): Delete = table.datastore.beforeInvoke(table.beforeInvoke(delete))
  def calling(table: Table, instructionType: InstructionType, sql: String): Unit = {
    table.calling(instructionType, sql)
    table.datastore.calling(instructionType, sql)
  }
  def calling(datastore: Datastore, instructionType: InstructionType, sql: String): Unit = {
    datastore.calling(instructionType, sql)
  }
  def afterInvoke[E, R](table: Table, query: Query[E, R]): Unit = {
    table.afterInvoke[E, R](query)
    table.datastore.afterInvoke[E, R](query)
  }
  def afterInvoke(table: Table, insert: InsertSingle): Unit = {
    table.afterInvoke(insert)
    table.datastore.afterInvoke(insert)
  }
  def afterInvoke(table: Table, insert: InsertMultiple): Unit = {
    table.afterInvoke(insert)
    table.datastore.afterInvoke(insert)
  }
  def afterInvoke(table: Table, merge: Merge): Unit = {
    table.afterInvoke(merge)
    table.datastore.afterInvoke(merge)
  }
  def afterInvoke(table: Table, update: Update): Unit = {
    table.afterInvoke(update)
    table.datastore.afterInvoke(update)
  }
  def afterInvoke(table: Table, delete: Delete): Unit = {
    table.afterInvoke(delete)
    table.datastore.afterInvoke(delete)
  }
}