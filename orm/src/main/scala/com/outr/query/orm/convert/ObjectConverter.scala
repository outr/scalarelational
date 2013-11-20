package com.outr.query.orm.convert

import com.outr.query.{QueryResult, Column}
import com.outr.query.property.ForeignKey
import com.outr.query.orm.ORMTable

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ObjectConverter[O](implicit manifest: Manifest[O]) extends ORMConverter[Int, O] {
  def fromORM(column: Column[Int], o: O): Conversion[Int, O] = if (o.asInstanceOf[AnyRef] != null) {
    val foreignColumn = ForeignKey(column).foreignColumn
    val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[O]]
    val updated = foreignTable.persist(o)
    val idColumnValue = foreignTable.idFor[Int](updated).getOrElse(throw new RuntimeException(s"No id was returned with $o (${o.getClass})"))
    Conversion(Some(column(idColumnValue.value)), Some(updated))
  } else {
    Conversion.empty
  }

  def toORM(column: Column[Int], c: Int, result: QueryResult): Option[O] = {
    val foreignColumn = ForeignKey(column).foreignColumn
    val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[O]]
    Some(foreignTable.result2Instance(result))
  }
}