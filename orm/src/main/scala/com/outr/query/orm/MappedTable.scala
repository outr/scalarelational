package com.outr.query.orm

import com.outr.query._
import com.outr.query.QueryResult
import com.outr.query.ColumnValue
import scala.Some
import org.powerscala.event.processor.{UnitProcessor, ModifiableOptionProcessor, ModifiableProcessor}
import org.powerscala.reflect.EnhancedClass
import org.powerscala.ref.WeakReference
import org.powerscala.event.Listenable
import com.outr.query.table.property.TableProperty

/**
 * MappedTable is the base-class used for mapping a class to a table.
 *
 * @author Matt Hicks <matt@outr.com>
 */
abstract class MappedTable[T](datastore: Datastore, name: String, tableProperties: TableProperty*)
                             (implicit val manifest: Manifest[T]) extends Table(datastore, name, tableProperties: _*) with Listenable {
  def this(datastore: Datastore, tableProperties: TableProperty*)(implicit manifest: Manifest[T]) = this(datastore, null.asInstanceOf[String], tableProperties: _*)

  MappedTable.synchronized {     // Map class to table so it can be found externally
    MappedTable.class2Table += clazz -> this
  }

  /**
   * Fired immediately before persisting an object to the database. The instance may be modified in the response.
   * Persisting is called before insert, merge, and update.
   */
  val persisting = new ModifiableProcessor[T]("persisting")
  /**
   * Fired immediately before inserting a new object into the database. The instance may be modified in the response.
   */
  val inserting = new ModifiableProcessor[T]("inserting")
  /**
   * Fired immediately before merging a new object into the database. The instance may be modified in the response.
   */
  val merging = new ModifiableProcessor[T]("merging")
  /**
   * Fired immediately before updating an object in the database. The instance may be modified in the response.
   */
  val updating = new ModifiableProcessor[T]("updating")
  /**
   * Fired immediately before deleting an object from the database. The instance may be modified or None returned to
   * avoid deletion.
   */
  val deleting = new ModifiableOptionProcessor[T]("deleting")
  /**
   * Fired immediate after successful persisting. Persisted is called after insert, merge, and update.
   */
  val persisted = new ModifiableProcessor[T]("persisted")
  /**
   * Fired immediate after successful insert.
   */
  val inserted = new UnitProcessor[T]("inserted")
  /**
   * Fired immediate after successful merge.
   */
  val merged = new UnitProcessor[T]("merged")
  /**
   * Fired immediately after successful update.
   */
  val updated = new UnitProcessor[T]("updated")
  /**
   * Fired immediately after successful delete.
   */
  val deleted = new UnitProcessor[T]("deleted")
  /**
   * Fired immediately after querying an instance. Listeners have the ability to modify the resulting instance.
   */
  val queried = new ModifiableProcessor[T]("queried")

  lazy val clazz: EnhancedClass = manifest.runtimeClass

  /**
   * Generates a query that will pull back all necessary fields and joins to represent the instance.
   */
  def q: Query

  /**
   * Retrieves a list of ColumnValue entries representing the primary keys for this table and their values based upon
   * the supplied instance.
   *
   * @param value the instance to get the primary keys from
   * @return List of primary keys tied to columns
   */
  def primaryKeysFor(value: T): List[ColumnValue[_]]

  /**
   * Responsible method for converting a QueryResult into an instance of T.
   *
   * @param result representation of result as an object T
   * @return T
   */
  def result2Object(result: QueryResult): T

  /**
   * Updates the supplied instance with a new id and returns the updated instance.
   *
   * @param t the instance to assign an id to
   * @param id the id to be assigned
   * @return a copy of the instance with the new id
   */
  def updateWithId(t: T, id: Int): T

  /**
   * Converts the supplied object to a MappedObject representing the modified object (during persistence) along with
   * the column values to send to the database.
   *
   * @param value the instance to be converted to row representation
   * @param onlyChanges true if only the changed values should be supplied
   * @return MappedObject[T]
   */
  def object2Row(value: T, onlyChanges: Boolean = true): MappedObject[T]

  private def cache = datastore.session.store.getOrSet(clazz, Map.empty[Any, WeakReference[AnyRef]])
  def cached(key: Any) = cache.get(key) match {
    case Some(ref) => ref.get.asInstanceOf[Option[T]]
    case None => None
  }
  def updateCached(key: Any, instance: T) = datastore.session.store(clazz) = cache + (key -> WeakReference(instance.asInstanceOf[AnyRef]))
  def clearCached(key: Any) = datastore.session.store(clazz) = cache - key

  /**
   * Inserts the supplied instance into the datastore.
   *
   * @param t the instance to insert
   * @return updated instance reflecting any changes resulting from the insert
   */
  def insert(t: T): T = {
    val modified = inserting.fire(persisting.fire(t))
    val mapped = object2Row(modified)
    val insert = Insert(mapped.columnValues)
    if (insert.values.isEmpty) {
      throw new RuntimeException(s"Cannot insert $t (${t.getClass.getName}")
    }
    val result = datastore.exec(insert).toList.headOption match {
      case Some(id) => updateWithId(mapped.updated, id)
      case None => mapped.updated
    }
    updateCached(idFor(result).get.value, result)   // Update the caching value
    val updated2 = persisted.fire(result)
    inserted.fire(updated2)
    updated2
  }

  /**
   * Updates the provided instance to the database.
   *
   * @param t the instance to update
   * @return updated instance reflecting any changes resulting from the update to the database
   */
  def update(t: T): T = {
    val modified = updating.fire(persisting.fire(t))
    val mapped = object2Row(modified)
    if (mapped.columnValues.nonEmpty) {
      val columnValue = idFor[Any](mapped.updated).getOrElse(throw new RuntimeException(s"No id found for $t"))
      val update = Update(mapped.columnValues, this).where(columnValue2Condition(columnValue))
      val updatedRows = datastore.exec(update)
      if (updatedRows != 1) {
        throw new RuntimeException(s"Attempt to update single instance failed. Updated $updated but expected to update 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
      }
      updateCached(idFor(mapped.updated).get.value, mapped.updated)
    }
    val updated2 = persisted.fire(mapped.updated)
    this.updated.fire(updated2)
    updated2
  }

  /**
   * Merges the instance into the database. If a row already exists matching the primary keys of the instance it will
   * be replaced, otherwise the record will be inserted.
   *
   * @param t the instance to merge
   * @return updated instance reflecting any changes resulting from the merge
   */
  def merge(t: T): T = {
    val modified = merging.fire(persisting.fire(t))
    val mapped = object2Row(modified, onlyChanges = false)
    val key = primaryKeys.head
    if (primaryKeys.tail.nonEmpty) {
      throw new RuntimeException(s"Cannot merge with more than one primary key ($tableName)!")
    }
    val merge = Merge(key, mapped.columnValues)
    datastore.exec(merge)
    updateCached(idFor(mapped.updated).get.value, mapped.updated)   // Update the caching value
    val updated2 = persisted.fire(mapped.updated)
    merged.fire(updated2)
    updated2
  }

  /**
   * Inserts or updates the supplied instance based on whether an id is assigned to the instance.
   *
   * @param t the instance to persist
   * @return updated instance reflecting any changes resulting from the persist
   */
  def persist(t: T): T = if (hasId(t)) {
    update(t)
  } else {
    insert(t)
  }

  /**
   * Deletes the supplied instance.
   *
   * @param t the instance to delete
   * @return true if the delete was successful - will only be false if "deleting" event processor returns None
   */
  def delete(t: T) = {
    deleting.fire(t) match {
      case Some(instance) => {
        val columnValue = idFor[Any](instance).getOrElse(throw new RuntimeException(s"No id found for $t"))
        val delete = Delete(this).where(columnValue2Condition(columnValue))
        val deleted = datastore.exec(delete)
        if (deleted != 1) {
          throw new RuntimeException(s"Attempt to delete single instance failed. Deleted $deleted records, but expected to delete 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
        }
        clearCached(idFor(instance).get.value)
        this.deleted.fire(instance)
        true
      }
      case None => false
    }
  }

  /**
   * Queries the datastore for results based upon the supplied query.
   *
   * @param query the query to utilize
   * @return iterator to cycle through instances derived from query result rows
   */
  def query(query: Query) = new ORMResultsIterator[T](datastore.exec(query), result2Object)

  /**
   * True if the supplied instance has an id assigned (meaning it has been persisted).
   */
  def hasId(instance: T) = idFor[Any](instance).nonEmpty

  /**
   * Returns an optional ColumnValue[C] for the supplied instance based on whether it has been persisted.
   */
  def idFor[C](instance: T): Option[ColumnValue[C]] = {
    if (primaryKeys.size != 1) {
      throw new RuntimeException(s"Cannot get the id for a table that doesn't have exactly one primary key (has ${primaryKeys.size} primary keys)")
    }
    val keys = primaryKeysFor(instance)
    if (keys.size == 0) {
      None
    } else {
      Some(keys.head.asInstanceOf[ColumnValue[C]])
    }
  }

  /**
   * Retrieves an instance by id.
   *
   * @param primaryKey the primary key to look up the instance
   * @return Option[T]
   */
  def byId(primaryKey: Any) = {
    if (primaryKeys.size != 1) {
      throw new RuntimeException(s"Cannot query by id for a table that doesn't have exactly one primary key (has ${primaryKeys.size} primary keys)")
    }
    val pk = primaryKeys.head.asInstanceOf[Column[Any]]
    val query = Query(*, this).where(pk === primaryKey)
    val results = this.query(query).toList
    if (results.nonEmpty && results.tail.nonEmpty) {
      throw new RuntimeException(s"Query byId for ${pk.name} == $primaryKey returned ${results.size} results.")
    }
    results.headOption
  }
}

object MappedTable extends Listenable {
  private var class2Table = Map.empty[EnhancedClass, MappedTable[_]]

  def apply[T](clazz: EnhancedClass) = get[T](clazz).getOrElse(throw new RuntimeException(s"Unable to find $clazz ORMTable mapping."))
  def get[T](clazz: EnhancedClass) = class2Table.get(clazz).asInstanceOf[Option[ORMTable[T]]]
  def contains(clazz: EnhancedClass) = class2Table.contains(clazz)
}

case class MappedObject[T](updated: T, columnValues: List[ColumnValue[_]])