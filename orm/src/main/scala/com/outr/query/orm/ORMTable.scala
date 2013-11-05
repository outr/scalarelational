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
import org.powerscala.event.processor.OptionProcessor

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class ORMTable[T](tableName: String)(implicit val manifest: Manifest[T], datastore: Datastore) extends Table(tableName = tableName)(datastore) {
  ORMTable.configure(datastore)

  val conversions = new OptionProcessor[(EnhancedClass, ColumnValue[_]), Any]("conversions")
  conversions.on {
    case (resultType, columnValue) => if (resultType.hasType(classOf[Lazy[_]])) {
      Some(DelayedLazy(columnValue.column.foreignKey.get.table.asInstanceOf[ORMTable[_]], columnValue.value))
    } else {
      None
    }
  }

  lazy val clazz: EnhancedClass = manifest.runtimeClass
  lazy val mappedColumns = Map(clazz.caseValues.map {
    case cv if cv.valueType.hasType(classOf[Lazy[_]]) => columns.find(c => c.name.toLowerCase.startsWith(cv.name.toLowerCase)) match {
      case Some(column) => column.asInstanceOf[Column[Any]] -> cv
      case None => throw new RuntimeException(s"Unable to find match for lazy '${cv.name}' in '$tableName'.")
    }
    case cv => column[Any](cv.name) match {
      case Some(column) => column -> cv
      case None => throw new RuntimeException(s"Unable to map '${cv.name}' to a column in '$tableName'.")
    }
  }: _*)

  def insert(instance: T): T = {
    val values = instance2Values(instance)
    val insert = Insert(values)
    datastore.exec(insert).toList.headOption match {
      case Some(id) => clazz.copy[T](instance, Map(autoIncrement.get.name -> id))
      case None => instance
    }
  }
  def query(query: Query) = new ORMResultsIterator[T](datastore.exec(query), this)
  def update(instance: T): T = {
    val values = instance2Values(instance)
    val conditions = Conditions(primaryKeysFor(instance).map(cv => cv.column === cv.value), ConnectType.And)
    val update = Update(values, this).where(conditions)
    val updated = datastore.exec(update)
    if (updated != 1) {
      throw new RuntimeException(s"Attempt to update single instance failed. Updated $updated but expected to update 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
    }
    instance
  }
  def delete(instance: T) = {
    val conditions = Conditions(primaryKeysFor(instance).map(cv => cv.column === cv.value), ConnectType.And)
    val delete = Delete(this).where(conditions)
    val deleted = datastore.exec(delete)
    if (deleted != 1) {
      throw new RuntimeException(s"Attempt to delete single instance failed. Deleted $deleted records, but exptected to delete 1 record. Primary Keys: ${primaryKeys.map(c => c.name).mkString(", ")}")
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

  def primaryKeyConditions(instance: T) = {
    Conditions(primaryKeysFor(instance).map(cv => cv.column === cv.value), ConnectType.And)
  }
  def primaryKeysFor(instance: T) = if (primaryKeys.nonEmpty) {
    primaryKeys.collect {
      case c if mappedColumns.contains(c.asInstanceOf[Column[Any]]) => {
        val caseValue = mappedColumns(c.asInstanceOf[Column[Any]])
        val value = caseValue[Any](instance.asInstanceOf[AnyRef])
        ColumnValue(c.asInstanceOf[Column[Any]], value)
      }
    }
  } else {
    throw new RuntimeException(s"No primary keys defined for $tableName")
  }

  def instance2Values(instance: T) = mappedColumns.map {
    case (column, caseValue) => ColumnValue[Any](column, caseValue[Any](instance.asInstanceOf[AnyRef]))
  }.toList

  protected[orm] def result2Instance(result: QueryResult): T = {
    val args = result.values.collect {
      case columnValue if mappedColumns.contains(columnValue.column.asInstanceOf[Column[Any]]) => {
        val caseValue = mappedColumns(columnValue.column.asInstanceOf[Column[Any]])
        val sqlValue = result.table.datastore.sqlValue2Value[Any](columnValue.column.asInstanceOf[Column[Any]], columnValue.value)
        val value = EnhancedMethod.convertToOption(columnValue.column.name, sqlValue, caseValue.valueType) match {
          case Some(r) => r
          case None => conversions.fire(caseValue.valueType -> ColumnValue(columnValue.column.asInstanceOf[Column[Any]], sqlValue)) match {
            case Some(modified) => modified
            case None => throw new RuntimeException(s"ORMTable.result2Instance: Unable to convert $sqlValue (${sqlValue.getClass.getSimpleName}) to ${caseValue.valueType}")
          }
        }
        caseValue.name -> value
      }
    }.toMap
    clazz.copy[T](null.asInstanceOf[T], args, requireValues = false)
  }
}

object ORMTable {
  private var configured = Set.empty[Datastore]

  def configure(datastore: Datastore) = synchronized {
    if (!configured.contains(datastore)) {
      datastore.value2SQL.on {
        case (column, value) => value match {
          case l: Lazy[_] => l.get() match {
            case Some(v) => {
              column.foreignKey match {
                case Some(foreignColumn) => foreignColumn.table match {
                  case ormt: ORMTable[_] => {
                    val keys = ormt.asInstanceOf[ORMTable[Any]].primaryKeysFor(v)
                    if (keys.size != 1) {
                      throw new RuntimeException(s"There must be exactly one primary key to use Lazy but in ${ormt.tableName} there are ${keys.size}.")
                    }
                    Some(keys.head.value)
                  }
                }
                case None => throw new RuntimeException(s"Lazy can only be used on columns that have a foreign key reference: ${column.name}")
              }
            }
            case None => null
          }
          case _ => None
        }
      }
      configured += datastore
    }
  }
}