package org.scalarelational.model

import org.powerscala.event.Listenable
import org.powerscala.event.processor.{UnitProcessor, ModifiableProcessor}
import org.scalarelational.instruction._

/**
 * ModularSupport can be optionally mixed into a Datastore and/or Table to receive and even modify calls to the the
 * database.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait ModularSupport extends SQLContainer with Listenable {
  object handlers {
    val inserting = new ModifiableProcessor[Insert]("inserting")
    val merging = new ModifiableProcessor[Merge]("merging")
    val updating = new ModifiableProcessor[Update]("updating")
    val deleting = new ModifiableProcessor[Delete]("deleting")
    val querying = new ModifiableProcessor[Query[_, _]]("querying")

    val inserted = new UnitProcessor[Insert]("inserted")
    val merged = new UnitProcessor[Merge]("merged")
    val updated = new UnitProcessor[Update]("updated")
    val deleted = new UnitProcessor[Delete]("deleted")
    val queried = new UnitProcessor[Query[_, _]]("queried")
  }

  import handlers._

  override protected def beforeInvoke[E, R](query: Query[E, R]): Query[E, R] = querying.fire(super.beforeInvoke(query)).asInstanceOf[Query[E, R]]

  override protected def beforeInvoke(insert: InsertSingle): InsertSingle = inserting.fire(super.beforeInvoke(insert)).asInstanceOf[InsertSingle]

  override protected def beforeInvoke(insert: InsertMultiple): InsertMultiple = inserting.fire(super.beforeInvoke(insert)).asInstanceOf[InsertMultiple]

  override protected def beforeInvoke(merge: Merge): Merge = merging.fire(super.beforeInvoke(merge))

  override protected def beforeInvoke(update: Update): Update = updating.fire(super.beforeInvoke(update))

  override protected def beforeInvoke(delete: Delete): Delete = deleting.fire(super.beforeInvoke(delete))

  override protected def afterInvoke[E, R](query: Query[E, R]): Unit = {
    super.afterInvoke(query)
    queried.fire(query)
  }

  override protected def afterInvoke(insert: InsertSingle): Unit = {
    super.afterInvoke(insert)
    inserted.fire(insert)
  }

  override protected def afterInvoke(insert: InsertMultiple): Unit = {
    super.afterInvoke(insert)
    inserted.fire(insert)
  }

  override protected def afterInvoke(merge: Merge): Unit = {
    super.afterInvoke(merge)
    merged.fire(merge)
  }

  override protected def afterInvoke(update: Update): Unit = {
    super.afterInvoke(update)
    updated.fire(update)
  }

  override protected def afterInvoke(delete: Delete): Unit = {
    super.afterInvoke(delete)
    deleted.fire(delete)
  }
}
