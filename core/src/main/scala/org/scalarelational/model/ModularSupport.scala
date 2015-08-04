package org.scalarelational.model

import org.powerscala.event.Listenable
import org.powerscala.event.processor.{UnitProcessor, ModifiableProcessor}
import org.scalarelational.datatype.DataTypes
import org.scalarelational.instruction._

/**
 * ModularSupport can be optionally mixed into a Datastore and/or Table to
 * receive and even modify calls to the the database.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait ModularSupport extends SQLContainer with Listenable with DataTypes {
  object handlers {
    val inserting = new ModifiableProcessor[Insert[_]]("inserting")
    val merging = new ModifiableProcessor[Merge]("merging")
    val updating = new ModifiableProcessor[Update[_]]("updating")
    val deleting = new ModifiableProcessor[Delete]("deleting")
    val querying = new ModifiableProcessor[Query[_, _]]("querying")

    val inserted = new UnitProcessor[Insert[_]]("inserted")
    val merged = new UnitProcessor[Merge]("merged")
    val updated = new UnitProcessor[Update[_]]("updated")
    val deleted = new UnitProcessor[Delete]("deleted")
    val queried = new UnitProcessor[Query[_, _]]("queried")
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
    queried.fire(query)
  }

  override protected def afterInvoke[T](insert: InsertSingle[T]) {
    super.afterInvoke(insert)
    inserted.fire(insert)
  }

  override protected def afterInvoke(insert: InsertMultiple) {
    super.afterInvoke(insert)
    inserted.fire(insert)
  }

  override protected def afterInvoke(merge: Merge) {
    super.afterInvoke(merge)
    merged.fire(merge)
  }

  override protected def afterInvoke[T](update: Update[T]) {
    super.afterInvoke(update)
    updated.fire(update)
  }

  override protected def afterInvoke(delete: Delete) {
    super.afterInvoke(delete)
    deleted.fire(delete)
  }
}
