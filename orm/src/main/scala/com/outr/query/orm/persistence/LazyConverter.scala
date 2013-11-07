package com.outr.query.orm.persistence

import com.outr.query.orm.{DelayedLazy, ORMTable, PreloadedLazy, Lazy}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object LazyConverter extends Converter {
  def convert2SQL(persistence: Persistence, value: Any) = {
    val lzy = value.asInstanceOf[Lazy[_]]
    if (lzy.get().nonEmpty) {
      value match {
        case l: PreloadedLazy[_] => {
          val foreignTable = persistence.column.foreignKey.get.table.asInstanceOf[ORMTable[Any]]
          val updated = foreignTable.persist(l())     // Update the lazy record into the database
          val id = foreignTable.idFor(updated).value
          ConversionResponse(id, Some(PreloadedLazy[Any](Some(updated))))
        }
        case delayed: DelayedLazy[_] => {
          val foreignTable = persistence.column.foreignKey.get.asInstanceOf[ORMTable[Any]]
          val value = delayed.apply()
          val id = foreignTable.idFor(value)
          ConversionResponse(id, Some(delayed))
        }
      }
    } else {
      EmptyConversion
    }
  }

  def convert2Value(persistence: Persistence, sql: Any) = {
    val foreignTable = persistence.column.foreignKey.get.table.asInstanceOf[ORMTable[Any]]
    Some(DelayedLazy(foreignTable, sql))
  }
}
