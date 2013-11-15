package com.outr.query.orm.persistence

import com.outr.query.orm._
import com.outr.query.orm.DelayedLazyList
import com.outr.query.{Table, QueryResult, Column}
import com.outr.query.property.ForeignKey
import org.powerscala.reflect.CaseValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
class LazyListConverter(table: ORMTable[Any], caseValue: CaseValue) extends Converter {
  case class M2MLazyListSupport(linkingTable: Table, otherColumnInLinkingTable: Column[Any], otherTable: ORMTable[Any], otherTablePrimaryKey: Column[Any])

  val primaryKey = table.primaryKeys.head.asInstanceOf[Column[Any]]
  val foreignColumn = table.lazyMappings.get(caseValue) match {
    case Some(fc) => fc.asInstanceOf[Column[Any]]
    case None => throw new RuntimeException(s"Unable to find ${caseValue.name} in ${table.tableName}.")
  }
  val m2mOption = if (foreignColumn.table.linking) {
    val linkingTable = foreignColumn.table
    val otherColumn = linkingTable.columns.find(c => c.has(ForeignKey.name) && c != foreignColumn).get.asInstanceOf[Column[Any]]
    val otherTable = otherColumn.prop[ForeignKey](ForeignKey.name).get.foreignColumn.table.asInstanceOf[ORMTable[Any]]
    val otherTablePrimaryKey = otherTable.primaryKeys.head.asInstanceOf[Column[Any]]
    Some(M2MLazyListSupport(linkingTable, otherColumn, otherTable, otherTablePrimaryKey))
  } else {
    None
  }

  if (m2mOption.nonEmpty) {
    table.persisted.on {
      case instance => postPersistence(instance)
    }
  }

  def postPersistence(instance: Any) = {
    val m2m = m2mOption.get
    val id = table.idFor(instance).value
    val lzy = caseValue[LazyList[Any]](instance.asInstanceOf[AnyRef])
    val firstValue = foreignColumn(id)
    lzy match {
      case l: PreloadedLazyList[_] => {
        l().foreach {
          case item => {
            val otherId = m2m.otherTable.idFor(item)
            val secondValue = m2m.otherColumnInLinkingTable(otherId)

            // Delete the record if it already exists
            m2m.linkingTable.datastore.exec(m2m.linkingTable.datastore.delete(m2m.linkingTable) where firstValue and secondValue)
            // Insert the linking record
            m2m.linkingTable.datastore.insert(firstValue, secondValue)
          }
        }
      }
      case l: DelayedLazyList[_] => // Nothing to do, records are already linked
    }
  }

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

  /* -- Must be *after* persist
  val updated = foreignTable.persist(item)
            val id = foreignTable.idFor(updated).value
            persistence.table.lazyMappings.get(persistence.caseValue) match {
              case Some(foreignColumn) if foreignColumn.table.linking => {    // Insert linking record
                val linkingTable = foreignColumn.table
                val otherColumn = linkingTable.columns.find(c => c.has(ForeignKey.name) && c != foreignColumn).get.asInstanceOf[Column[Any]]
                val firstColumnValue = otherColumn(id)
                val secondColumnValue = persistence.table.
//                linkingTable.datastore.insert()
                println(s"**** convert2SQL: $id - ${foreignColumn.longName} - $clazz - ${linkingTable.tableName} - $otherColumn - $foreignTable")
                // TODO: insert linking record
              }
              case _ =>
            }
   */

  def convert2Value(persistence: Persistence, sql: Any, args: Map[String, Any], query: QueryResult) = {
    args.get(primaryKey.name) match {
      case Some(id) => {
        m2mOption match {
          case Some(m2m) => {
            val query = m2m.otherTable.q innerJoin m2m.linkingTable on m2m.otherTablePrimaryKey === m2m.otherColumnInLinkingTable where foreignColumn === id
            Some(DelayedLazyList[Any](m2m.otherTable, query)(Manifest.classType[Any](m2m.otherTable.clazz.javaClass)))
          }
          case None => {
            val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[Any]]
            val query = foreignTable.q where foreignColumn.asInstanceOf[Column[Any]] === id
            Some(DelayedLazyList[Any](foreignTable, query)(Manifest.classType[Any](foreignTable.clazz.javaClass)))
          }
        }
      }
      case None => None     // Looks like the primary key isn't part of the query results
    }
  }
}