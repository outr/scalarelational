package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.instruction.{Instruction, Query}
import org.scalarelational.model.{Column, Table}
import org.scalarelational.result.QueryResult

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object mapper {
  implicit class MappableQuery(query: Query) {
    def mapped[R](f: QueryResult => R): Stream[R] = query.result.toStream.map(qr => f(qr))

    def as[R](implicit manifest: Manifest[R]): Stream[R] = {
      val clazz: EnhancedClass = manifest.runtimeClass
      val f = (r: QueryResult) => {
        clazz.create[R](r.toSimpleMap)
      }
      mapped[R](f)
    }
  }

  implicit class MappableTable(table: Table) {
    def persist[T <: AnyRef](value: T, forceInsert: Boolean = false)(implicit manifest: Manifest[T]): Instruction[Int] = {
      val clazz: EnhancedClass = manifest.runtimeClass
      val values = clazz.caseValues.flatMap(cv => table.getColumn[Any](cv.name).map(c => c(cv[Any](value))))
      val primaryColumn = table.primaryKeys.head.asInstanceOf[Column[Any]]
      values.find(cv => cv.column == primaryColumn) match {
        case Some(primaryKey) => {
          val exists = primaryKey.value match {
            case _ if forceInsert => false
            case None => false
            case null => false
            case i: Int if i < 1 => false
            case _ => true
          }
          if (exists) {
            // Update
            val updates = values.filterNot(cv => cv.column == primaryColumn)
            table.datastore.update(values: _*) where (primaryColumn === primaryKey.value)
          } else {
            table.datastore.insert(values: _*)
          }
        }
        case None => table.datastore.insert(values: _*)
      }

    }
  }
}