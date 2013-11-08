package com.outr.query.orm

import com.outr.query._
import org.powerscala.reflect._
import com.outr.query.QueryResult
import com.outr.query.ColumnValue
import com.outr.query.Update
import com.outr.query.Conditions
import scala.Some
import com.outr.query.Delete
import com.outr.query.Query
import com.outr.query.Column
import com.outr.query.Insert
import org.powerscala.event.processor.ModifiableProcessor
import com.outr.query.orm.persistence.{ConversionResponse, EmptyConversion, DefaultConverter, Persistence}
import org.powerscala.Priority
import org.powerscala.event.Listenable

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class ORMTable[T](tableName: String)(implicit val manifest: Manifest[T], datastore: Datastore) extends Table(tableName = tableName)(datastore) {
  lazy val clazz: EnhancedClass = manifest.runtimeClass
  lazy val caseValues = clazz.caseValues
  lazy val persistence = loadPersistence()
  lazy val column2PersistenceMap = Map(persistence.map(p => p.column.asInstanceOf[Column[Any]] -> p): _*)

  private var _lazyMappings = Map.empty[CaseValue, Column[_]]
  def lazyMappings = _lazyMappings

  ORMTable.synchronized {     // Map class to table so it can be found externally
    ORMTable.class2Table += clazz -> this
  }
  def map(fieldName: String, foreignColumn: Column[_]) = synchronized {
    val caseValue = caseValues.find(cv => cv.name.equalsIgnoreCase(fieldName)).getOrElse(throw new RuntimeException(s"Unable to find $fieldName in $clazz."))
    _lazyMappings += caseValue -> foreignColumn
  }

  def insert(instance: T): T = {
    val (updated, values) = instance2ColumnValues(instance)
    val insert = Insert(values)
    datastore.exec(insert).toList.headOption match {
      case Some(id) => clazz.copy[T](updated, Map(autoIncrement.get.name -> id))
      case None => updated
    }
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
  def update(instance: T): T = {
    val (modified, values) = instance2ColumnValues(instance)
    val conditions = Conditions(primaryKeysFor(modified).map(cv => cv.column === cv.value), ConnectType.And)
    val update = Update(values, this).where(conditions)
    val updated = datastore.exec(update)
    if (updated != 1) {
      throw new RuntimeException(s"Attempt to update single instance failed. Updated $updated but expected to update 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
    }
    modified
  }
  def delete(instance: T) = {
    val conditions = Conditions(primaryKeysFor(instance).map(cv => cv.column === cv.value), ConnectType.And)
    val delete = Delete(this).where(conditions)
    val deleted = datastore.exec(delete)
    if (deleted != 1) {
      throw new RuntimeException(s"Attempt to delete single instance failed. Deleted $deleted records, but exptected to delete 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
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
      throw new RuntimeException(s"Unable to create persistence mapping for ${cv.name} (${cv.valueType.simpleName}). No converter found for ${persistence.column.name} (${persistence.column.manifest.runtimeClass.getSimpleName}).")
    }
    persistence
  }

  protected def instance2ColumnValues(instance: T) = {
    var updated = instance
    val columnValues = persistence.map {
      case p => {
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
      case columnValue => column2PersistenceMap.get(columnValue.column.asInstanceOf[Column[Any]]) match {
        case Some(p) => p.converter.convert2Value(p, columnValue.value, args) match {
          case Some(v) => args += p.caseValue.name -> v
          case None => // No value in the case class for this column
        }
        case None => throw new RuntimeException(s"Unable to column: ${columnValue.column.name} in persistence map!")
      }
    }
    // Process fields in case class that have no direct column association
    result.table.asInstanceOf[ORMTable[Any]].persistence.foreach {
      case p => if (p.column == null) {
        p.converter.convert2Value(p, null, args) match {
          case Some(v) => args += p.caseValue.name -> v
          case None => // No value in the case class for this column
        }
      }
    }
    clazz.copy[T](null.asInstanceOf[T], args)
  }
}

object ORMTable extends Listenable {
  private var class2Table = Map.empty[EnhancedClass, ORMTable[_]]

  def apply[T](clazz: EnhancedClass) = get[T](clazz).getOrElse(throw new RuntimeException(s"Unable to find $clazz ORMTable mapping."))
  def get[T](clazz: EnhancedClass) = class2Table.get(clazz).asInstanceOf[Option[ORMTable[T]]]

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
  persistenceSupport.listen(Priority.Lowest) {    // DefaultConvert is used if no other converter is set
    case persistence => if (persistence.converter == null) {
      persistence.copy(converter = DefaultConverter)
    } else {
      persistence
    }
  }
}