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
    val values = mappedColumns.map {
      case (column, caseValue) => ColumnValue[Any](column, caseValue[Any](instance.asInstanceOf[AnyRef]))
    }.toList
    val insert = Insert(values)
    table.datastore.exec(insert).toList.headOption match {
      case Some(id) => clazz.copy[T](instance, Map(table.autoIncrement.get.name -> id))
      case None => instance
    }
  }
  def query(query: Query) = new ORMResultsIterator[T](table.datastore.exec(query), this)

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
