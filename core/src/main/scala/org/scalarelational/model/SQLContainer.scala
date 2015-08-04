package org.scalarelational.model

import org.scalarelational.instruction._
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLContainer {
  protected def beforeInvoke[E, R](query: Query[E, R]): Query[E, R] = query
  protected def beforeInvoke[T](insert: InsertSingle[T]): InsertSingle[T] = insert
  protected def beforeInvoke(insert: InsertMultiple): InsertMultiple = insert
  protected def beforeInvoke(merge: Merge): Merge = merge
  protected def beforeInvoke[T](update: Update[T]): Update[T] = update
  protected def beforeInvoke(delete: Delete): Delete = delete
  protected def calling(instructionType: InstructionType, sql: String) {}
  protected def afterInvoke[E, R](query: Query[E, R]) {}
  protected def afterInvoke[T](insert: InsertSingle[T]) {}
  protected def afterInvoke(insert: InsertMultiple) {}
  protected def afterInvoke(merge: Merge) {}
  protected def afterInvoke[T](update: Update[T]) {}
  protected def afterInvoke(delete: Delete) {}
}

object SQLContainer {
  def beforeInvoke[T, E, R](table: Table, query: Query[E, R]): Query[E, R] =
    table.datastore.beforeInvoke[E, R](table.beforeInvoke[E, R](query))

  def beforeInvoke[T](table: Table, insert: InsertSingle[T]): InsertSingle[T] =
    table.datastore.beforeInvoke(table.beforeInvoke(insert))

  def beforeInvoke(table: Table, insert: InsertMultiple): InsertMultiple =
    table.datastore.beforeInvoke(table.beforeInvoke(insert))

  def beforeInvoke(table: Table, merge: Merge): Merge =
    table.datastore.beforeInvoke(table.beforeInvoke(merge))

  def beforeInvoke[T](table: Table, update: Update[T]): Update[T] =
    table.datastore.beforeInvoke(table.beforeInvoke(update))

  def beforeInvoke(table: Table, delete: Delete): Delete =
    table.datastore.beforeInvoke(table.beforeInvoke(delete))

  def calling(table: Table, instructionType: InstructionType, sql: String) {
    table.calling(instructionType, sql)
    table.datastore.calling(instructionType, sql)
  }

  def calling(datastore: Datastore, instructionType: InstructionType, sql: String) {
    datastore.calling(instructionType, sql)
  }

  def afterInvoke[T, E, R](table: Table, query: Query[E, R]) {
    table.afterInvoke[E, R](query)
    table.datastore.afterInvoke[E, R](query)
  }

  def afterInvoke[T](table: Table, insert: InsertSingle[T]) {
    table.afterInvoke(insert)
    table.datastore.afterInvoke(insert)
  }

  def afterInvoke(table: Table, insert: InsertMultiple) {
    table.afterInvoke(insert)
    table.datastore.afterInvoke(insert)
  }

  def afterInvoke(table: Table, merge: Merge) {
    table.afterInvoke(merge)
    table.datastore.afterInvoke(merge)
  }

  def afterInvoke[T](table: Table, update: Update[T]) {
    table.afterInvoke(update)
    table.datastore.afterInvoke(update)
  }

  def afterInvoke(table: Table, delete: Delete) {
    table.afterInvoke(delete)
    table.datastore.afterInvoke(delete)
  }
}