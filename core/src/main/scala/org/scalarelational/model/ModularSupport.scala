package org.scalarelational.model

import com.outr.props.Channel
import org.scalarelational.datatype.DataTypeSupport
import org.scalarelational.instruction._

import scala.collection.mutable.ArrayBuffer

/**
 * ModularSupport can be optionally mixed into a Datastore and/or Table to
 * receive and even modify calls to the the database.
 */
trait ModularSupport extends SQLContainer with DataTypeSupport {
  class Processor[T] {
    val functions = ArrayBuffer.empty[T => T]
    def fire(value: T): T =
      functions.foldLeft(value) { case (acc, cur) => cur(acc) }
    def attach(f: T => T): Unit = functions += f
  }

  object Processor {
    def apply[T](): Processor[T] = new Processor[T]
  }

  object handlers {
    val inserting = Processor[Insert[_]]()
    val merging = Processor[Merge]()
    val updating = Processor[Update[_]]()
    val deleting = Processor[Delete]()
    val querying = Processor[Query[_, _]]()

    val inserted = Channel[Insert[_]]()
    val merged = Channel[Merge]()
    val updated = Channel[Update[_]]()
    val deleted = Channel[Delete]()
    val queried = Channel[Query[_, _]]()
  }

  import handlers._

  override protected def beforeInvoke[E, R](query: Query[E, R]): Query[E, R] =
    querying.fire(super.beforeInvoke(query)).asInstanceOf[Query[E, R]]

  override protected def beforeInvoke[T](insert: InsertSingle[T]): InsertSingle[T] =
    inserting.fire(super.beforeInvoke(insert)).asInstanceOf[InsertSingle[T]]

  override protected def beforeInvoke(insert: InsertMultiple): InsertMultiple =
    inserting.fire(super.beforeInvoke(insert)).asInstanceOf[InsertMultiple]

  override protected def beforeInvoke(merge: Merge): Merge =
    merging.fire(super.beforeInvoke(merge))

  override protected def beforeInvoke[T](update: Update[T]): Update[T] =
    updating.fire(super.beforeInvoke(update)).asInstanceOf[Update[T]]

  override protected def beforeInvoke(delete: Delete): Delete =
    deleting.fire(super.beforeInvoke(delete))

  override protected def afterInvoke[E, R](query: Query[E, R]) {
    super.afterInvoke(query)
    queried := query
  }

  override protected def afterInvoke[T](insert: InsertSingle[T]) {
    super.afterInvoke(insert)
    inserted := insert
  }

  override protected def afterInvoke(insert: InsertMultiple) {
    super.afterInvoke(insert)
    inserted := insert
  }

  override protected def afterInvoke(merge: Merge) {
    super.afterInvoke(merge)
    merged := merge
  }

  override protected def afterInvoke[T](update: Update[T]) {
    super.afterInvoke(update)
    updated := update
  }

  override protected def afterInvoke(delete: Delete) {
    super.afterInvoke(delete)
    deleted := delete
  }
}
