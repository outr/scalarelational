package org.scalarelational.model

import org.scalarelational.instruction._
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLContainer {
  protected def beforeInvoke[E, R](query: Query[E, R]): Query[E, R] = query
  protected def beforeInvoke[T](insert: InsertSingle[T]): InsertSingle[T] = insert
  protected def beforeInvoke[T](insert: InsertMultiple[T]): InsertMultiple[T] = insert
  protected def beforeInvoke[T](merge: Merge[T]): Merge[T] = merge
  protected def beforeInvoke[T](update: Update[T]): Update[T] = update
  protected def beforeInvoke[T](delete: Delete[T]): Delete[T] = delete
  protected def calling(instructionType: InstructionType, sql: String) {}
  protected def afterInvoke[E, R](query: Query[E, R]) {}
  protected def afterInvoke[T](insert: InsertSingle[T]) {}
  protected def afterInvoke[T](insert: InsertMultiple[T]) {}
  protected def afterInvoke[T](merge: Merge[T]) {}
  protected def afterInvoke[T](update: Update[T]) {}
  protected def afterInvoke[T](delete: Delete[T]) {}
}

object SQLContainer {
  def beforeInvoke[T, E, R](table: Table[T], query: Query[E, R]): Query[E, R] =
    table.datastore.beforeInvoke[E, R](table.beforeInvoke[E, R](query))

  def beforeInvoke[T](table: Table[T], insert: InsertSingle[T]): InsertSingle[T] =
    table.datastore.beforeInvoke(table.beforeInvoke(insert))

  def beforeInvoke[T](table: Table[T], insert: InsertMultiple[T]): InsertMultiple[T] =
    table.datastore.beforeInvoke(table.beforeInvoke(insert))

  def beforeInvoke[T](table: Table[T], merge: Merge[T]): Merge[T] =
    table.datastore.beforeInvoke(table.beforeInvoke(merge))

  def beforeInvoke[T](table: Table[T], update: Update[T]): Update[T] =
    table.datastore.beforeInvoke(table.beforeInvoke(update))

  def beforeInvoke[T](table: Table[T], delete: Delete[T]): Delete[T] =
    table.datastore.beforeInvoke(table.beforeInvoke(delete))

  def calling[T](table: Table[T], instructionType: InstructionType, sql: String) {
    table.calling(instructionType, sql)
    table.datastore.calling(instructionType, sql)
  }

  def calling(datastore: Datastore, instructionType: InstructionType, sql: String) {
    datastore.calling(instructionType, sql)
  }

  def afterInvoke[T, E, R](table: Table[T], query: Query[E, R]) {
    table.afterInvoke[E, R](query)
    table.datastore.afterInvoke[E, R](query)
  }

  def afterInvoke[T](table: Table[T], insert: InsertSingle[T]) {
    table.afterInvoke(insert)
    table.datastore.afterInvoke(insert)
  }

  def afterInvoke[T](table: Table[T], insert: InsertMultiple[T]) {
    table.afterInvoke(insert)
    table.datastore.afterInvoke(insert)
  }

  def afterInvoke[T](table: Table[T], merge: Merge[T]) {
    table.afterInvoke(merge)
    table.datastore.afterInvoke(merge)
  }

  def afterInvoke[T](table: Table[T], update: Update[T]) {
    table.afterInvoke(update)
    table.datastore.afterInvoke(update)
  }

  def afterInvoke[T](table: Table[T], delete: Delete[T]) {
    table.afterInvoke(delete)
    table.datastore.afterInvoke(delete)
  }
}