package com.outr.query.orm

import com.outr.query._
import org.powerscala.reflect._
import com.outr.query.ColumnValue
import scala.Some
import com.outr.query.Insert

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ORMTable[T] private[orm](val table: Table)(implicit manifest: Manifest[T]) {
  lazy val clazz: EnhancedClass = manifest.runtimeClass
  lazy val mappedColumns = clazz.caseValues.map {
    case cv => table.column[Any](cv.name).map(c => c -> cv)
  }.flatten.toMap

  def insert(instance: T): T = {
    val values = instance2Values(instance)
    val insert = Insert(values)
    table.datastore.exec(insert).toList.headOption match {
      case Some(id) => clazz.copy[T](instance, Map(table.autoIncrement.get.name -> id))
      case None => instance
    }
  }
  def query(query: Query) = new ORMResultsIterator[T](table.datastore.exec(query), this)
  def update(instance: T): T = {
    val values = instance2Values(instance)
    val conditions = Conditions(primaryKeys(instance).map(cv => cv.column === cv.value), ConnectType.And)
    val update = Update(values, table).where(conditions)
    val updated = table.datastore.exec(update)
    if (updated != 1) {
      throw new RuntimeException(s"Attempt to update single instance failed. Updated $updated but expected to update 1 record. Primary Keys: ${table.primaryKeys.map(c => c.name).mkString(", ")}")
    }
    instance
  }
  def delete(instance: T) = {
    val conditions = Conditions(primaryKeys(instance).map(cv => cv.column === cv.value), ConnectType.And)
    val delete = Delete(table).where(conditions)
    val deleted = table.datastore.exec(delete)
    if (deleted != 1) {
      throw new RuntimeException(s"Attempt to delete single instance failed. Deleted $deleted records, but exptected to delete 1 record. Primary Keys: ${table.primaryKeys.map(c => c.name).mkString(", ")}")
    }
  }

  def primaryKeys(instance: T) = if (table.primaryKeys.nonEmpty) {
    table.primaryKeys.collect {
      case c if mappedColumns.contains(c.asInstanceOf[Column[Any]]) => ColumnValue(c.asInstanceOf[Column[Any]], mappedColumns(c.asInstanceOf[Column[Any]])[Any](instance.asInstanceOf[AnyRef]))
    }
  } else {
    throw new RuntimeException(s"No primary keys defined for ${table.tableName}")
  }

  def instance2Values(instance: T) = mappedColumns.map {
    case (column, caseValue) => ColumnValue[Any](column, caseValue[Any](instance.asInstanceOf[AnyRef]))
  }.toList

  protected[orm] def result2Instance(result: QueryResult): T = {
    val args = result.values.collect {
      case columnValue if mappedColumns.contains(columnValue.column.asInstanceOf[Column[Any]]) => {
        val caseValue = mappedColumns(columnValue.column.asInstanceOf[Column[Any]])
        caseValue.name -> columnValue.value
      }
    }.toMap
    clazz.copy[T](null.asInstanceOf[T], args, requireValues = false)
  }
}
