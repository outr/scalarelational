package com.outr.query.orm.convert

import com.outr.query.orm.{PreloadedLazy, ORMTable, DelayedLazy, Lazy}
import com.outr.query.{QueryResult, Column}
import com.outr.query.column.property.ForeignKey

/**
 * @author Matt Hicks <matt@outr.com>
 */
class LazyConverter[O](implicit manifest: Manifest[O]) extends ORMConverter[Int, Lazy[O]] {
  def fromORM(column: Column[Int], lzy: Lazy[O]): Conversion[Int, Lazy[O]] = {
    val foreignColumn = ForeignKey(column).foreignColumn
    val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[O]]
    if (lzy.get().nonEmpty) {
      lzy match {
        case l: PreloadedLazy[O] => {
          val updated = foreignTable.persist(l())
          val id = foreignTable.idFor[Int](updated).get.value
          ConversionResponse(Some(column(id)), Some(PreloadedLazy[O](Some(updated))))
        }
        case l: DelayedLazy[O] => {
          val id = foreignTable.idFor[Int](l()).get.value
          ConversionResponse(Some(column(id)), Some(l))
        }
      }
    } else {
      Conversion.empty
    }
  }

  def toORM(column: Column[Int], c: Int, result: QueryResult): Option[Lazy[O]] = if (c > 0) {
    val foreignColumn = ForeignKey(column).foreignColumn
    val foreignTable = foreignColumn.table.asInstanceOf[ORMTable[O]]
    Some(DelayedLazy(foreignTable, c))
  } else {
    None
  }
}