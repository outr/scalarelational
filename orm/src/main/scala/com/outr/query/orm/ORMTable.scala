package com.outr.query.orm

import com.outr.query._
import org.powerscala.reflect._
import org.powerscala.event.processor.{ModifiableOptionProcessor, UnitProcessor, ModifiableProcessor}
import org.powerscala.event.Listenable
import com.outr.query.Column
import com.outr.query.property.{ColumnProperty, ForeignKey}
import org.powerscala.ref.WeakReference
import com.outr.query.orm.convert._
import com.outr.query.convert.ColumnConverter
import scala.collection.mutable.ListBuffer

import scala.language.existentials
import com.outr.query.Update
import scala.Some
import com.outr.query.QueryResult
import com.outr.query.orm.convert.ConversionResponse
import com.outr.query.orm.persistence.Persistence
import com.outr.query.Delete
import com.outr.query.ColumnValue
import com.outr.query.Query
import com.outr.query.Insert

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class ORMTable[T](tableName: String)(implicit val manifest: Manifest[T], datastore: Datastore) extends Table(tableName = tableName)(datastore) {
  private val _persistence = ListBuffer.empty[Persistence[T, _, _]]

  lazy val clazz: EnhancedClass = manifest.runtimeClass
  lazy val caseValues = clazz.caseValues
  def persistence = _persistence.toList
  lazy val column2PersistenceMap = Map(persistence.map(p => p.column.asInstanceOf[Column[Any]] -> p): _*)

  implicit val optionInt2IntConverter = OptionInt2IntConverter

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
  val persisted = new ModifiableProcessor[T]("persisted")
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
  /**
   * Fired immediately after querying an instance. Listeners have the ability to modify the resulting instance.
   */
  val queried = new ModifiableProcessor[T]("queried")

  def orm[C, O](name: String,
                caseValue: CaseValue,
                columnConverter: ColumnConverter[C],
                ormConverter: ORMConverter[C, O],
                properties: ColumnProperty*)
               (implicit columnManifest: Manifest[C]) = synchronized {
    val column = this.column[C](name, columnConverter, properties: _*)
    _persistence += Persistence[T, C, O](this, caseValue, column, ormConverter)
    column
  }

  def orm[C, O](name: String, properties: ColumnProperty*)
               (implicit columnConverter: ColumnConverter[C], ormConverter: ORMConverter[C, O], columnManifest: Manifest[C]) = {
    val column = this.column[C](name, columnConverter, properties: _*)
    val caseValue = clazz.caseValue(name).getOrElse(throw new RuntimeException(s"Unable to find $name in $clazz"))
    _persistence += Persistence[T, C, O](this, caseValue, column, ormConverter)
    column
  }

  def orm[C, O](name: String, fieldName: String, properties: ColumnProperty*)
               (implicit columnConverter: ColumnConverter[C], ormConverter: ORMConverter[C, O], columnManifest: Manifest[C]) = {
    val column = this.column[C](name, columnConverter, properties: _*)
    val caseValue = clazz.caseValue(fieldName).getOrElse(throw new RuntimeException(s"Unable to find $fieldName in $clazz"))
    _persistence += Persistence[T, C, O](this, caseValue, column, ormConverter)
    column
  }

  def orm[C, O](name: String, fieldName: String, ormConverter: ORMConverter[C, O], properties: ColumnProperty*)
               (implicit columnConverter: ColumnConverter[C], columnManifest: Manifest[C]) = {
    val column = this.column[C](name, columnConverter, properties: _*)
    val caseValue = clazz.caseValue(fieldName).getOrElse(throw new RuntimeException(s"Unable to find $fieldName in $clazz"))
    _persistence += Persistence[T, C, O](this, caseValue, column, ormConverter)
    column
  }

  def orm[C](name: String, properties: ColumnProperty*)
            (implicit columnConverter: ColumnConverter[C], manifest: Manifest[C]) = {
    val column = this.column[C](name, columnConverter, properties: _*)
    val caseValue = clazz.caseValue(name).getOrElse(throw new RuntimeException(s"Unable to find $name in $clazz"))
    _persistence += Persistence[T, C, C](this, caseValue, column, new SameTypeORMConverter[C](column))
    column
  }

  def orm[C](name: String, fieldName: String, properties: ColumnProperty*)
            (implicit columnConverter: ColumnConverter[C], manifest: Manifest[C]) = {
    val column = this.column[C](name, columnConverter, properties: _*)
    val caseValue = clazz.caseValue(fieldName).getOrElse(throw new RuntimeException(s"Unable to find $fieldName in $clazz"))
    _persistence += Persistence[T, C, C](this, caseValue, column, new SameTypeORMConverter[C](column))
    column
  }

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
    updateCached(idFor(result).get.value, result)   // Update the caching value
    val updated2 = persisted.fire(result)
    inserted.fire(updated2)
    updated2
  }
  def query(query: Query) = new ORMResultsIterator[T](datastore.exec(query), this)
  def persist(instance: T): T = if (hasId(instance)) {
    update(instance)
  } else {
    insert(instance)
  }
  def hasId(instance: T) = idFor[Any](instance).nonEmpty
  def update(t: T): T = {
    val modified = updating.fire(persisting.fire(t))
    val (updated, values) = instance2ColumnValues(modified)
    if (values.nonEmpty) {
      val columnValue = idFor[Any](updated).getOrElse(throw new RuntimeException(s"No id found for $t"))
      val update = Update(values, this).where(columnValue2Condition(columnValue))
      val updatedRows = datastore.exec(update)
      if (updatedRows != 1) {
        throw new RuntimeException(s"Attempt to update single instance failed. Updated $updated but expected to update 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
      }
      updateCached(idFor(updated).get.value, updated)
    }
    val updated2 = persisted.fire(updated)
    this.updated.fire(updated2)
    updated2
  }
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
  def primaryKeysFor(instance: T) = if (primaryKeys.nonEmpty) {
    primaryKeys.collect {
      case column => {
        val c = column.asInstanceOf[Column[Any]]
        column2PersistenceMap.get(c) match {
          case Some(p) => {
            val v = p.caseValue[Any](instance.asInstanceOf[AnyRef])
            p.converter.asInstanceOf[ORMConverter[Any, Any]].fromORM(c, v) match {
              case r: ConversionResponse[_, _] => r.columnValue
              case _ => None
            }
          }
          case None => None
        }
//        column2PersistenceMap.get(c).map(p => ColumnValue(c, p.caseValue[Any](instance.asInstanceOf[AnyRef]), None))
      }
    }.flatten
  } else {
    throw new RuntimeException(s"No primary keys defined for $tableName")
  }
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

  protected def instance2ColumnValues(instance: T) = {
    var updated = instance
    val cached = idFor[Any](instance) match {
      case Some(columnValue) => this.cached(columnValue.value).map(t => clazz.diff(t.asInstanceOf[AnyRef], instance.asInstanceOf[AnyRef]).map(t => t._1).toSet).getOrElse(null)
      case None => null
    }
    val columnValues = persistence.collect {
      case p if cached == null || cached.contains(p.caseValue) => {
        p.conversion(instance) match {
          case EmptyConversion => None
          case response: ConversionResponse[_, _] => {
            response.updated match {
              case Some(updatedValue) => {      // Modify the instance with an updated value
                updated = p.caseValue.copy(updated.asInstanceOf[AnyRef], updatedValue).asInstanceOf[T]
              }
              case None => // Nothing to do
            }
            response.columnValue match {
              case Some(cv) => Some(cv.asInstanceOf[ColumnValue[Any]])
              case None => None
            }
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
        case Some(p) => p.asInstanceOf[Persistence[T, Any, Any]].conversion(columnValue.asInstanceOf[ColumnValue[Any]], result) match {
          case Some(v) => args += p.caseValue.name -> v
          case None => // No value in the case class for this column
        }
        case None => // Possible for columns to be returned that don't map to persistence
      }
      case v => throw new RuntimeException(s"${v.getClass} is not supported in ORM results.")
    }
//    // Process fields in case class that have no direct column association
//    result.table.asInstanceOf[ORMTable[Any]].persistence.foreach {
//      case p => if (p.column == null) {
//        p.conversion()
//        p.converter.convert2Value(p, null, args, result) match {
//          case Some(v) => args += p.caseValue.name -> v
//          case None => // No value in the case class for this column
//        }
//      }
//    }
    val instance = clazz.copy[T](null.asInstanceOf[T], args)
    idFor[Any](instance) match {
      case Some(columnValue) => updateCached(columnValue.value, instance)
      case None => // No id, so we can't update the cache
    }
    queried.fire(instance)      // Allow listener to update the resulting instance before returning
  }
}

object ORMTable extends Listenable {
  private var class2Table = Map.empty[EnhancedClass, ORMTable[_]]

  def apply[T](clazz: EnhancedClass) = get[T](clazz).getOrElse(throw new RuntimeException(s"Unable to find $clazz ORMTable mapping."))
  def get[T](clazz: EnhancedClass) = class2Table.get(clazz).asInstanceOf[Option[ORMTable[T]]]
  def contains(clazz: EnhancedClass) = class2Table.contains(clazz)

//  val persistenceSupport = new ModifiableProcessor[Persistence]("persistenceSupport")
//  persistenceSupport.listen(Priority.High) {      // Direct mapping of CaseValue -> Column
//    case persistence => if (persistence.column == null) {
//      persistence.table.column[Any](persistence.caseValue.name) match {
//        case Some(column) => persistence.copy(column = column)
//        case None => persistence
//      }
//    } else {
//      persistence
//    }
//  }
//  private val defaultTypes = List("Int", "Long", "String", "Array[Byte]", "scala.Option")
//  persistenceSupport.listen(Priority.Lowest) {    // DefaultConvert is used if no other converter is set
//    case persistence => if (persistence.column != null && persistence.converter == null && defaultTypes.contains(persistence.caseValue.valueType.name)) {
//      persistence.copy(converter = DefaultConverter)
//    } else {
//      persistence
//    }
//  }
//  persistenceSupport.listen(Priority.Low) {       // Case Class conversion
//    case persistence => if (persistence.column == null && persistence.caseValue.valueType.isCase && contains(persistence.caseValue.valueType)) {
//      val name = persistence.caseValue.name
//      val column = persistence.table.columnsByName[Any](s"${name}_id", s"${name}id", s"${name}_fk", s"${name}fk").collect {
//        case c if c.has(ForeignKey.name) => c
//      }.headOption.getOrElse(throw new RuntimeException(s"Unable to find foreign key column for ${persistence.table.tableName}.${persistence.caseValue.name} (Lazy)"))
//      persistence.copy(column = column, converter = CaseClassConverter)
//    } else {
//      persistence
//    }
//  }
//  persistenceSupport.listen(Priority.Low) {       // EnumEntry conversion
//    case persistence => if (persistence.column != null && persistence.caseValue.valueType.hasType(classOf[EnumEntry]) && persistence.converter == null) {
//      persistence.copy(converter = new EnumEntryConverter(persistence.caseValue.valueType))
//    } else {
//      persistence
//    }
//  }
//
//  // Make sure Lazy and LazyList have added their persistence support
//  Lazy
//  LazyList
}