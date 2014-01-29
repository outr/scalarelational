package com.outr.query.orm

import com.outr.query._
import org.powerscala.reflect._
import org.powerscala.event.processor.ModifiableProcessor
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

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class ORMTable[T](tableName: String)
                          (implicit manifest: Manifest[T],
                           datastore: Datastore) extends MappedTable[T](tableName)(manifest, datastore) {
  private val _persistence = ListBuffer.empty[Persistence[T, _, _]]

  lazy val caseValues = clazz.caseValues
  def persistence = _persistence.toList
  lazy val column2PersistenceMap = Map(persistence.map(p => p.column.asInstanceOf[Column[Any]] -> p): _*)

  implicit val optionInt2IntConverter = new Option2ValueConverter[Int]

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

  def orm[C, O](name: String, columnConverter: ColumnConverter[C], properties: ColumnProperty*)
               (implicit ormConverter: ORMConverter[C, O], columnManifest: Manifest[C]) = {
    val column = this.column[C](name, columnConverter, properties: _*)
    val caseValue = clazz.caseValue(name).getOrElse(throw new RuntimeException(s"Unable to find $name in $clazz"))
    _persistence += Persistence[T, C, O](this, caseValue, column, ormConverter)
    column
  }

  def orm[C, O](name: String, columnConverter: ColumnConverter[C], ormConverter: ORMConverter[C, O], properties: ColumnProperty*)
               (implicit columnManifest: Manifest[C]) = {
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

  def orm[C](name: String, columnConverter: ColumnConverter[C], properties: ColumnProperty*)
            (implicit manifest: Manifest[C]) = {
    val column = this.column[C](name, columnConverter, properties: _*)
    val caseValue = clazz.caseValue(name).getOrElse(throw new RuntimeException(s"Unable to find $name in $clazz"))
    _persistence += Persistence[T, C, C](this, caseValue, column, new SameTypeORMConverter[C](column))
    column
  }

  private var _lazyMappings = Map.empty[CaseValue, Column[_]]
  def lazyMappings = _lazyMappings

  lazy val q = {
    var s = datastore.select(*) from this
    persistence.foreach {
      case p if p.caseValue.valueType.isCase && MappedTable.contains(p.caseValue.valueType) => {
        val table = MappedTable[Any](p.caseValue.valueType)
        s = s.fields(table.*) leftJoin table on p.column.asInstanceOf[Column[Any]] === ForeignKey(p.column).foreignColumn.asInstanceOf[Column[Any]]
      }
      case _ => // Ignore
    }
    s
  }

  def map(fieldName: String, foreignColumn: Column[_]) = synchronized {
    val caseValue = caseValues.find(cv => cv.name.equalsIgnoreCase(fieldName)).getOrElse(throw new RuntimeException(s"Unable to find $fieldName in $clazz."))
    _lazyMappings += caseValue -> foreignColumn
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
      }
    }.flatten
  } else {
    throw new RuntimeException(s"No primary keys defined for $tableName")
  }

  def object2Row(instance: T, onlyChanges: Boolean = true) = {
    var updated = instance
    val cached = if (onlyChanges) {
      idFor[Any](instance) match {
        case Some(columnValue) => this.cached(columnValue.value).map(t => clazz.diff(t.asInstanceOf[AnyRef], instance.asInstanceOf[AnyRef]).map(t => t._1).toSet).getOrElse(null)
        case None => null
      }
    } else {
      null
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
    MappedObject(updated, columnValues)
  }

  def result2Object(result: QueryResult): T = {
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
    val instance = clazz.copy[T](null.asInstanceOf[T], args)
    idFor[Any](instance) match {
      case Some(columnValue) => updateCached(columnValue.value, instance)
      case None => // No id, so we can't update the cache
    }
    queried.fire(instance)      // Allow listener to update the resulting instance before returning
  }
}