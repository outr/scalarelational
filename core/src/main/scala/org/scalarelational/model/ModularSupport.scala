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
trait ModularSupport extends Listenable {
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
