package com.outr.query.orm

import com.outr.query._
import com.outr.query.convert.ColumnConverter
import com.outr.query.orm.convert._
import com.outr.query.property.{ForeignKey, ColumnProperty}
import org.powerscala.reflect._
import scala.collection.mutable.ListBuffer
import scala.language.existentials
import com.outr.query.orm.persistence.Persistence
import com.outr.query.QueryResult
import com.outr.query.ColumnValue
import com.outr.query.orm.convert.ConversionResponse
import com.outr.query.table.property.TableProperty

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class ORMTable[T](datastore: Datastore, name: String, tableProperties: TableProperty*)(implicit manifest: Manifest[T])
                extends MappedTable[T](datastore, name, tableProperties: _*)(manifest) {
  def this(datastore: Datastore, tableProperties: TableProperty*)(implicit manifest: Manifest[T]) = this(datastore, null.asInstanceOf[String], tableProperties: _*)

  private val _ormColumns = ListBuffer.empty[ORMColumn[T, _, _]]
  def ormColumns = _ormColumns.toList

  private var ormPersistence = Map.empty[EnhancedClass, ORMPersistence[T]]

  implicit val optionInt2IntConverter = new Option2ValueConverter[Int]

  // Looks up or generates an ORMPersistence instance for a concrete case class
  protected def persistenceFor(caseClass: EnhancedClass) = synchronized {
    ormPersistence.get(caseClass) match {
      case Some(p) => p
      case None => {
        val p = new ORMPersistence[T](this, caseClass)
        ormPersistence += caseClass -> p
        p
      }
    }
  }

  /**
   * Determines the class representation for the supplied QueryResult row. Overriding this allows for polymorphism to
   * function properly on this table. Defaults to using table's type.
   *
   * @param result the class representation for this row
   * @return EnhancedClass
   */
  def caseClassForRow(result: QueryResult) = clazz

  def orm[C, F](columnName: String,
                fieldName: String,
                columnConverter: ColumnConverter[C],
                ormConverter: ORMConverter[C, F],
                properties: ColumnProperty*)
               (implicit columnManifest: Manifest[C], fieldManifest: Manifest[F]): Column[C] = synchronized {
    val column = this.column[C](columnName, columnConverter, properties: _*)
    val ormColumn = ORMColumn(this, column, fieldName, columnConverter, ormConverter, properties.toList, columnManifest.runtimeClass, fieldManifest.runtimeClass)
    _ormColumns += ormColumn
    column
  }

  def orm[C, F](name: String, properties: ColumnProperty*)
               (implicit columnConverter: ColumnConverter[C],
                ormConverter: ORMConverter[C, F],
                columnManifest: Manifest[C], fieldManifest: Manifest[F]): Column[C] = {
    orm[C, F](name, name, columnConverter, ormConverter, properties: _*)
  }

  def orm[C, F](name: String, columnConverter: ColumnConverter[C], properties: ColumnProperty*)
               (implicit ormConverter: ORMConverter[C, F],
                columnManifest: Manifest[C],
                fieldManifest: Manifest[F]): Column[C] = {
    orm[C, F](name, name, columnConverter, ormConverter, properties: _*)
  }

  def orm[C, F](name: String,
                columnConverter: ColumnConverter[C],
                ormConverter: ORMConverter[C, F],
                properties: ColumnProperty*)
               (implicit columnManifest: Manifest[C], fieldManifest: Manifest[F]): Column[C] = {
    orm[C, F](name, name, columnConverter, ormConverter, properties: _*)
  }

  def orm[C, F](columnName: String, fieldName: String, properties: ColumnProperty*)
               (implicit columnConverter: ColumnConverter[C],
                ormConverter: ORMConverter[C, F],
                columnManifest: Manifest[C],
                fieldManifest: Manifest[F]): Column[C] = {
    orm[C, F](columnName, fieldName, columnConverter, ormConverter, properties: _*)
  }

  def orm[C, F](columnName: String, fieldName: String, ormConverter: ORMConverter[C, F], properties: ColumnProperty*)
               (implicit columnConverter: ColumnConverter[C],
                columnManifest: Manifest[C],
                fieldManifest: Manifest[F]): Column[C] = {
    orm[C, F](columnName, fieldName, columnConverter, ormConverter, properties: _*)
  }

  def orm[C](name: String, properties: ColumnProperty*)
            (implicit columnConverter: ColumnConverter[C], manifest: Manifest[C]): Column[C] = {
    orm[C, C](name, name, columnConverter, new SameTypeORMConverter[C], properties: _*)
  }

  def orm[C](columnName: String, fieldName: String, properties: ColumnProperty*)
            (implicit columnConverter: ColumnConverter[C], manifest: Manifest[C]) = {
    orm[C, C](columnName, fieldName, columnConverter, new SameTypeORMConverter[C], properties: _*)
  }

  def orm[C](name: String, columnConverter: ColumnConverter[C], properties: ColumnProperty*)
            (implicit manifest: Manifest[C]) = {
    orm[C, C](name, name, columnConverter, new SameTypeORMConverter[C], properties: _*)
  }

  override def object2Row(instance: T, onlyChanges: Boolean) = {
    var updated = instance
    val cached = if (onlyChanges) {
      idFor[Any](instance) match {
        case Some(columnValue) => this.cached(columnValue.value).map(t => clazz.diff(t.asInstanceOf[AnyRef], instance.asInstanceOf[AnyRef]).map(t => t._1).toSet).getOrElse(null)
        case None => null
      }
    } else {
      null
    }
    val ormPersistence = persistenceFor(instance.getClass)
    val columnValues = ormPersistence.persistence.collect {
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

  override def updateWithId(t: T, id: Int) = t.getClass.copy[T](t, Map(autoIncrement.get.name -> id))

  protected def hasFieldsForThisTable(result: QueryResult) = result.values.find {
    case cv: ColumnValue[_] => cv.column.table == this
    case _ => false
  }.nonEmpty

  override def result2Object(result: QueryResult) = if (hasFieldsForThisTable(result)) {
    // Lookup ORMPersistence
    val caseClass = caseClassForRow(result)
    val ormPersistence = persistenceFor(caseClass)

    var args = Map.empty[String, Any]
    // Process query result columns
    result.values.foreach {
      case columnValue: ColumnValue[_] => ormPersistence.column2PersistenceMap.get(columnValue.column.asInstanceOf[Column[Any]]) match {
        case Some(p) => p.asInstanceOf[Persistence[T, Any, Any]].conversion(columnValue.asInstanceOf[ColumnValue[Any]], result) match {
          case Some(v) => args += p.caseValue.name -> v
          case None => // No value in the case class for this column
        }
        case None => // Possible for columns to be returned that don't map to persistence
      }
      case v => throw new RuntimeException(s"${v.getClass} is not supported in ORM results.")
    }
    val instance = caseClass.copy[T](null.asInstanceOf[T], args)
    idFor[Any](instance) match {
      case Some(columnValue) => updateCached(columnValue.value, instance)
      case None => // No id, so we can't update the cache
    }
    queried.fire(instance)      // Allow listener to update the resulting instance before returning
  } else {
    null.asInstanceOf[T]
  }

  def primaryKeysFor(instance: T) = if (primaryKeys.nonEmpty) {
    val ormPersistence = persistenceFor(instance.getClass)

    primaryKeys.collect {
      case column => {
        val c = column.asInstanceOf[Column[Any]]
        ormPersistence.column2PersistenceMap.get(c) match {
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

  lazy val q = {
    var query = datastore.select(*) from this
    ormColumns.foreach {
      case ormColumn if MappedTable.contains(ormColumn.fieldClass) => {
        val table = MappedTable[Any](ormColumn.fieldClass)
        query = query.fields(table.*) leftJoin table on ormColumn.column.asInstanceOf[Column[Any]] === ForeignKey(ormColumn.column).foreignColumn.asInstanceOf[Column[Any]]
      }
      case _ => // Nothing special to do for this column
    }
    query
  }
}

case class ORMColumn[T, C, F](table: ORMTable[T],
                              column: Column[C],
                              fieldName: String,
                              columnConverter: ColumnConverter[C],
                              ormConverter: ORMConverter[C, F],
                              properties: List[ColumnProperty],
                              columnClass: EnhancedClass,
                              fieldClass: EnhancedClass) {
  def persistence(caseValue: CaseValue) = Persistence(table, caseValue, column, ormConverter)
}

class ORMPersistence[T](table: ORMTable[T], caseClass: EnhancedClass) {
  val caseValues = caseClass.caseValues.map(cv => cv.name -> cv).toMap
  val persistence = table.ormColumns.collect {
    case ormColumn if caseValues.contains(ormColumn.fieldName) => ormColumn.persistence(caseValues(ormColumn.fieldName))
    case ormColumn if !ormColumn.column.has(MappingOptional) => throw new RuntimeException(s"Unable to find case value in $caseClass for ${ormColumn.fieldName}.")
  }
  val column2PersistenceMap = Map(persistence.map(p => p.column.asInstanceOf[Column[Any]] -> p): _*)
}