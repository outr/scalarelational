package com.outr.query.orm.persistence

import com.outr.query.orm._
import com.outr.query.orm.DelayedLazyList
import com.outr.query.{Column, Conditions}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object LazyListConverter extends Converter {
  def convert2SQL(persistence: Persistence, value: Any) = {
    val lzy = value.asInstanceOf[LazyList[_]]
    val clazz = lzy.clazz
    val foreignTable = ORMTable[Any](clazz)
    lzy match {
      case l: PreloadedLazyList[_] => { // Persist the records
        l().foreach {
          case item => foreignTable.persist(item)
        }
      }
      case l: DelayedLazyList[_] => // Nothing to do, these are already stored in the database
    }
    EmptyConversion
  }

  def convert2Value(persistence: Persistence, sql: Any, args: Map[String, Any]) = {
    val name = persistence.caseValue.name.toLowerCase
    persistence.table.many2One.find(c => { println(s"m2o: ${c.name} - $name"); c.name.toLowerCase.startsWith(name) }) match {
      case Some(foreignColumn) => {
        val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[Any]]
        val primaryKey = persistence.table.primaryKeys.head.asInstanceOf[Column[Any]]
        val id = args(primaryKey.name)      // TODO: support better lookup support
        val conditions = Conditions(List(primaryKey === id))
        Some(DelayedLazyList(foreignTable, conditions))
      }
      case None => throw new RuntimeException(s"Unable to find many2one mapping for LazyList - ${persistence.table.tableName}.${persistence.caseValue.name}")
    }
  }
}