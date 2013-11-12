package com.outr.query.orm.persistence

import com.outr.query.orm.ORMTable
import com.outr.query.QueryResult

/**
 * @author Matt Hicks <matt@outr.com>
 */
object CaseClassConverter extends Converter {
  def convert2SQL(persistence: Persistence, value: Any) = if (value != null) {
    val ormTable = persistence.column.foreignKey.get.table.asInstanceOf[ORMTable[Any]]
    val updated = ormTable.persist(value)
    val id = ormTable.idFor(updated)
    ConversionResponse(id, Some(updated))
  } else {
    EmptyConversion
  }

  def convert2Value(persistence: Persistence, sql: Any, args: Map[String, Any], query: QueryResult) = if (sql != null) {
    val ormTable = persistence.column.foreignKey.get.table.asInstanceOf[ORMTable[Any]]
    Some(ormTable.result2Instance(query))
  } else {
    None
  }
}
