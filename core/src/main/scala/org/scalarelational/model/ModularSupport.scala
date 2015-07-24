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
    val inserting = new ModifiableProcessor[Insert]("inserting")
    val merging = new ModifiableProcessor[Merge[_]]("merging")
    val updating = new ModifiableProcessor[Update[_]]("updating")
    val deleting = new ModifiableProcessor[Delete[_]]("deleting")
    val querying = new ModifiableProcessor[Query[_, _]]("querying")

    val inserted = new UnitProcessor[Insert]("inserted")
    val merged = new UnitProcessor[Merge[_]]("merged")
    val updated = new UnitProcessor[Update[_]]("updated")
    val deleted = new UnitProcessor[Delete[_]]("deleted")
    val queried = new UnitProcessor[Query[_, _]]("queried")
  }

  import handlers._

  override protected def beforeInvoke[E, R](query: Query[E, R]): Query[E, R] =
    querying.fire(super.beforeInvoke(query)).asInstanceOf[Query[E, R]]

  override protected def beforeInvoke[T](insert: InsertSingle[T]): InsertSingle[T] =
    inserting.fire(super.beforeInvoke(insert)).asInstanceOf[InsertSingle[T]]

  override protected def beforeInvoke[T](insert: InsertMultiple[T]): InsertMultiple[T] =
    inserting.fire(super.beforeInvoke(insert)).asInstanceOf[InsertMultiple[T]]

  override protected def beforeInvoke[T](merge: Merge[T]): Merge[T] =
    merging.fire(super.beforeInvoke(merge)).asInstanceOf[Merge[T]]

  override protected def beforeInvoke[T](update: Update[T]): Update[T] =
    updating.fire(super.beforeInvoke(update)).asInstanceOf[Update[T]]

  override protected def beforeInvoke[T](delete: Delete[T]): Delete[T] =
    deleting.fire(super.beforeInvoke(delete)).asInstanceOf[Delete[T]]

  override protected def afterInvoke[E, R](query: Query[E, R]) {
    super.afterInvoke(query)
    queried.fire(query)
  }

  override protected def afterInvoke[T](insert: InsertSingle[T]) {
    super.afterInvoke(insert)
    inserted.fire(insert)
  }

  override protected def afterInvoke[T](insert: InsertMultiple[T]) {
    super.afterInvoke(insert)
    inserted.fire(insert)
  }

  override protected def afterInvoke[T](merge: Merge[T]) {
    super.afterInvoke(merge)
    merged.fire(merge)
  }

  override protected def afterInvoke[T](update: Update[T]) {
    super.afterInvoke(update)
    updated.fire(update)
  }

  override protected def afterInvoke[T](delete: Delete[T]) {
    super.afterInvoke(delete)
    deleted.fire(delete)
  }
}
