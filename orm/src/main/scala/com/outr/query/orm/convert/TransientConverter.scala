package com.outr.query.orm.convert

import com.outr.query.orm._
import com.outr.query.Column
import com.outr.query.property.ForeignKey
import com.outr.query.QueryResult
import scala.Some

/**
 * @author Matt Hicks <matt@outr.com>
 */
class TransientConverter[O](implicit manifest: Manifest[O]) extends ORMConverter[Int, Transient[O]] {
  def fromORM(column: Column[Int], t: Transient[O]): Conversion[Int, Transient[O]] = {
    val foreignColumn = ForeignKey(column).foreignColumn
    val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[O]]
    t match {
      case p: PreloadedTransient[O] => p.value match {
        case Some(value) => {
          val updated = foreignTable.persist(value)
          val id = foreignTable.idFor[Int](updated).get.value
          ConversionResponse(Some(column(id)), Some(PreloadedTransient[O](Some(updated))))
        }
        case None => Conversion.empty
      }
      case d: DynamicTransient[O] => {
        val id = d.key.asInstanceOf[Int]
        ConversionResponse(Some(column(id)), Some(d))
      }
    }
  }

  def toORM(column: Column[Int], c: Int, result: QueryResult): Option[Transient[O]] = {
    val foreignColumn = ForeignKey(column).foreignColumn
    val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[O]]
    Some(DynamicTransient(foreignTable, c))
  }
}