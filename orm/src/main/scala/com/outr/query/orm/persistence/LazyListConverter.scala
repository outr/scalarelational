package com.outr.query.orm.persistence

import com.outr.query.orm._
import com.outr.query.orm.DelayedLazyList
import com.outr.query.{QueryResult, Column, Conditions}

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

  def convert2Value(persistence: Persistence, sql: Any, args: Map[String, Any], query: QueryResult) = {
    persistence.table.lazyMappings.get(persistence.caseValue) match {
      case Some(foreignColumn) => {
        val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[Any]]
        val primaryKey = persistence.table.primaryKeys.head.asInstanceOf[Column[Any]]
        args.get(primaryKey.name) match {
          case Some(id) => {
            val conditions = Conditions(List(foreignColumn.asInstanceOf[Column[Any]] === id))
            Some(DelayedLazyList[Any](foreignTable, conditions)(Manifest.classType[Any](foreignTable.clazz.javaClass)))
          }
          case None => None     // Looks like the primary key isn't part of the query results
        }
      }
      case None => throw new RuntimeException(s"Unable to find ${persistence.caseValue.name} in ${persistence.table.tableName}.")
    }
  }
}