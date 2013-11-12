package com.outr.query.orm.persistence

import com.outr.query.orm.ORMTable

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

  def convert2Value(persistence: Persistence, sql: Any, args: Map[String, Any]) = {
    val ormTable = persistence.column.foreignKey.get.table.asInstanceOf[ORMTable[Any]]
    ormTable.byId(sql)
  }
}
