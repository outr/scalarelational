package com.outr.query.orm

import com.outr.query._
import org.powerscala.reflect._
import org.powerscala.event.processor.{ModifiableOptionProcessor, UnitProcessor, ModifiableProcessor}
import com.outr.query.orm.persistence._
import org.powerscala.Priority
import org.powerscala.event.Listenable
import com.outr.query.Update
import com.outr.query.orm.persistence.Persistence
import scala.Some
import com.outr.query.Delete
import com.outr.query.Column
import com.outr.query.QueryResult
import com.outr.query.ColumnValue
import com.outr.query.orm.persistence.ConversionResponse
import com.outr.query.Conditions
import com.outr.query.Query
import com.outr.query.Insert
import com.outr.query.property.ForeignKey
import org.powerscala.ref.WeakReference
import org.powerscala.enum.EnumEntry

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class ORMTable[T](tableName: String)(implicit val manifest: Manifest[T], datastore: Datastore) extends Table(tableName = tableName)(datastore) {
  lazy val clazz: EnhancedClass = manifest.runtimeClass
  lazy val caseValues = clazz.caseValues
  lazy val persistence = loadPersistence()
  lazy val column2PersistenceMap = Map(persistence.map(p => p.column.asInstanceOf[Column[Any]] -> p): _*)

  /**
   * Fired immediately before persisting an object to the database. The instance may be modified in the response.
   * Persisting is called before both insert and update.
   */
  val persisting = new ModifiableProcessor[T]("persisting")
  /**
   * Fired immediately before inserting a new object into the database. The instance may be modified in the response.
   */
  val inserting = new ModifiableProcessor[T]("inserting")
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
   * Fired immediate after successful persisting. Persisted is called after both insert and update.
   */
  val persisted = new UnitProcessor[T]("persisted")
  /**
   * Fired immediate after successful insert.
   */
  val inserted = new UnitProcessor[T]("inserted")
  /**
   * Fired immediately after successful update.
   */
  val updated = new UnitProcessor[T]("updated")
  /**
   * Fired immediately after successful delete.
   */
  val deleted = new UnitProcessor[T]("deleted")

  private def cache = datastore.session.store.getOrSet(clazz, Map.empty[Any, WeakReference[AnyRef]])
  def cached(key: Any) = cache.get(key) match {
    case Some(ref) => ref.get.asInstanceOf[Option[T]]
    case None => None
  }
  def updateCached(key: Any, instance: T) = datastore.session.store(clazz) = cache + (key -> WeakReference(instance.asInstanceOf[AnyRef]))
  def clearCached(key: Any) = datastore.session.store(clazz) = cache - key

  private var _lazyMappings = Map.empty[CaseValue, Column[_]]
  def lazyMappings = _lazyMappings

  lazy val q = {
    var s = datastore.select(*) from this
    persistence.foreach {
      case p if p.caseValue.valueType.isCase && ORMTable.contains(p.caseValue.valueType) => {
        val table = ORMTable[Any](p.caseValue.valueType)
        s = s.fields(table.*) innerJoin table on p.column.asInstanceOf[Column[Any]] === ForeignKey(p.column).foreignColumn.asInstanceOf[Column[Any]]
      }
      case _ => // Ignore
    }
    s
  }

  ORMTable.synchronized {     // Map class to table so it can be found externally
    ORMTable.class2Table += clazz -> this
  }
  def map(fieldName: String, foreignColumn: Column[_]) = synchronized {
    val caseValue = caseValues.find(cv => cv.name.equalsIgnoreCase(fieldName)).getOrElse(throw new RuntimeException(s"Unable to find $fieldName in $clazz."))
    _lazyMappings += caseValue -> foreignColumn
  }

  def insert(t: T): T = {
    val modified = inserting.fire(persisting.fire(t))
    val (updated, values) = instance2ColumnValues(modified)
    val insert = Insert(values)
    val result = datastore.exec(insert).toList.headOption match {
      case Some(id) => clazz.copy[T](updated, Map(autoIncrement.get.name -> id))
      case None => updated
    }
    updateCached(idFor(result).value, result)   // Update the caching value
    persisted.fire(result)
    inserted.fire(result)
    result
  }
  def query(query: Query) = new ORMResultsIterator[T](datastore.exec(query), this)
  def persist(instance: T): T = if (hasId(instance)) {
    update(instance)
  } else {
    insert(instance)
  }
  def hasId(instance: T): Boolean = {
    val id = idFor(instance).value
    id match {
      case Some(value) => true
      case None => false
      case 0 => false
      case i: Int if i > 0 => true
    }
  }
  def update(t: T): T = {
    val modified = updating.fire(persisting.fire(t))
    val (updated, values) = instance2ColumnValues(modified)
    if (values.nonEmpty) {
      val conditions = Conditions(primaryKeysFor(updated).map(cv => cv.column === cv.value), ConnectType.And)
      val update = Update(values, this).where(conditions)
      val updatedRows = datastore.exec(update)
      if (updatedRows != 1) {
        throw new RuntimeException(s"Attempt to update single instance failed. Updated $updated but expected to update 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
      }
      updateCached(idFor(updated).value, updated)
    }
    persisted.fire(updated)
    this.updated.fire(updated)
    updated
  }
  def delete(t: T) = {
    deleting.fire(t) match {
      case Some(instance) => {
        val conditions = Conditions(primaryKeysFor(instance).map(cv => cv.column === cv.value), ConnectType.And)
        val delete = Delete(this).where(conditions)
        val deleted = datastore.exec(delete)
        if (deleted != 1) {
          throw new RuntimeException(s"Attempt to delete single instance failed. Deleted $deleted records, but expected to delete 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
        }
        clearCached(idFor(instance).value)
        this.deleted.fire(instance)
        true
      }
      case None => false
    }
  }
  def primaryKeysFor(instance: T) = if (primaryKeys.nonEmpty) {
    primaryKeys.collect {
      case column => {
        val c = column.asInstanceOf[Column[Any]]
        column2PersistenceMap.get(c).map(p => ColumnValue(c, p.caseValue[Any](instance.asInstanceOf[AnyRef])))
      }
    }.flatten
  } else {
    throw new RuntimeException(s"No primary keys defined for $tableName")
  }
  def idFor(instance: T) = {
    val keys = primaryKeysFor(instance)
    if (keys.size != 1) {
      throw new RuntimeException(s"Cannot get the id for a table that doesn't have exactly one primary key (has ${primaryKeys.size} primary keys)")
    }
    keys.head
  }
  def byId(primaryKey: Any) = {
    if (primaryKeys.size != 1) {
      throw new RuntimeException(s"Cannot query by id for a table that doesn't have exactly one primary key (has ${primaryKeys.size} primary keys)")
    }
    val pk = primaryKeys.head.asInstanceOf[Column[Any]]
    val query = Query(*, this).where(pk === primaryKey)
    val results = this.query(query).toList
    if (results.tail.nonEmpty) {
      throw new RuntimeException(s"Query byId for ${pk.name} == $primaryKey returned ${results.size} results.")
    }
    results.headOption
  }

  protected def loadPersistence() = {
    caseValues.map {
      case cv => caseValue2Persistence(cv)
    }
  }

  protected def caseValue2Persistence[V](cv: CaseValue): Persistence = {
    val persistence = ORMTable.persistenceSupport.fire(Persistence(this, cv))
    if (persistence.converter == null) {
      throw new RuntimeException(s"Unable to create persistence mapping for $clazz.${cv.name} (${cv.valueType.simpleName}). No converter found for column ${persistence.column}.")
    }
    persistence
  }

  protected def instance2ColumnValues(instance: T) = {
    var updated = instance
    val cached = this.cached(idFor(instance).value).map(t => clazz.diff(t.asInstanceOf[AnyRef], instance.asInstanceOf[AnyRef]).map(t => t._1).toSet).getOrElse(null)
    val columnValues = persistence.collect {
      case p if cached == null || cached.contains(p.caseValue) => {
        val value = p.caseValue[Any](instance.asInstanceOf[AnyRef])
        p.converter.convert2SQL(p, value) match {
          case EmptyConversion => None
          case response: ConversionResponse => {
            response.updated match {
              case Some(updatedValue) => {      // Modify the instance with an updated value
                updated = p.caseValue.copy(updated.asInstanceOf[AnyRef], updatedValue).asInstanceOf[T]
              }
              case None => // Nothing to do
            }
            Some(p.column.asInstanceOf[Column[Any]](response.value))
          }
        }
      }
    }.flatten
    updated -> columnValues
  }

  protected[orm] def result2Instance(result: QueryResult): T = {
    var args = Map.empty[String, Any]
    // Process query result columns
    result.values.foreach {
      case columnValue: ColumnValue[_] => column2PersistenceMap.get(columnValue.column.asInstanceOf[Column[Any]]) match {
        case Some(p) => p.converter.convert2Value(p, columnValue.value, args, result) match {
          case Some(v) => args += p.caseValue.name -> v
          case None => // No value in the case class for this column
        }
        case None => // Possible for columns to be returned that don't map to persistence
      }
      case v => throw new RuntimeException(s"${v.getClass} is not supported in ORM results.")
    }
    // Process fields in case class that have no direct column association
    result.table.asInstanceOf[ORMTable[Any]].persistence.foreach {
      case p => if (p.column == null) {
        p.converter.convert2Value(p, null, args, result) match {
          case Some(v) => args += p.caseValue.name -> v
          case None => // No value in the case class for this column
        }
      }
    }
    val instance = clazz.copy[T](null.asInstanceOf[T], args)
    updateCached(idFor(instance).value, instance)
    instance
  }
}

object ORMTable extends Listenable {
  private var class2Table = Map.empty[EnhancedClass, ORMTable[_]]

  def apply[T](clazz: EnhancedClass) = get[T](clazz).getOrElse(throw new RuntimeException(s"Unable to find $clazz ORMTable mapping."))
  def get[T](clazz: EnhancedClass) = class2Table.get(clazz).asInstanceOf[Option[ORMTable[T]]]
  def contains(clazz: EnhancedClass) = class2Table.contains(clazz)

  val persistenceSupport = new ModifiableProcessor[Persistence]("persistenceSupport")
  persistenceSupport.listen(Priority.High) {      // Direct mapping of CaseValue -> Column
    case persistence => if (persistence.column == null) {
      persistence.table.column[Any](persistence.caseValue.name) match {
        case Some(column) => persistence.copy(column = column)
        case None => persistence
      }
    } else {
      persistence
    }
  }
  private val defaultTypes = List("Int", "Long", "String", "Array[Byte]", "scala.Option")
  persistenceSupport.listen(Priority.Lowest) {    // DefaultConvert is used if no other converter is set
    case persistence => if (persistence.column != null && persistence.converter == null && defaultTypes.contains(persistence.caseValue.valueType.name)) {
      persistence.copy(converter = DefaultConverter)
    } else {
      persistence
    }
  }
  persistenceSupport.listen(Priority.Low) {       // Case Class conversion
    case persistence => if (persistence.column == null && persistence.caseValue.valueType.isCase && contains(persistence.caseValue.valueType)) {
      val name = persistence.caseValue.name
      val column = persistence.table.columnsByName[Any](s"${name}_id", s"${name}id", s"${name}_fk", s"${name}fk").collect {
        case c if c.has(ForeignKey.name) => c
      }.headOption.getOrElse(throw new RuntimeException(s"Unable to find foreign key column for ${persistence.table.tableName}.${persistence.caseValue.name} (Lazy)"))
      persistence.copy(column = column, converter = CaseClassConverter)
    } else {
      persistence
    }
  }
  persistenceSupport.listen(Priority.Low) {       // EnumEntry conversion
    case persistence => if (persistence.column != null && persistence.caseValue.valueType.hasType(classOf[EnumEntry]) && persistence.converter == null) {
      persistence.copy(converter = new EnumEntryConverter(persistence.caseValue.valueType))
    } else {
      persistence
    }
  }

  // Make sure Lazy and LazyList have added their persistence support
  Lazy
  LazyList
}