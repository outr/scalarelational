package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.instruction.{InsertSingle, Instruction, Query}
import org.scalarelational.model.{Datastore, Column, Table}
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
    def persist[T <: AnyRef](value: T, forceInsert: Boolean = false)(implicit manifest: Manifest[T]): Instruction[T] = {
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
            val update = table.datastore.update(updates: _*) where (primaryColumn === primaryKey.value)
            new InstanceInstruction[T](update, value, table.datastore)
          } else {
            val primaryKeyCaseValue = clazz.caseValue(primaryColumn.name).getOrElse(throw new RuntimeException(s"Unable to find case value for ${primaryColumn.name} in $clazz."))
            new PersistInsertInstruction[T](table.datastore.insert(values: _*), primaryKeyCaseValue, value)
          }
        }
        case None => new InstanceInstruction[T](table.datastore.insert(values: _*), value, table.datastore)
      }
    }
  }

  class InstanceInstruction[T](instruction: Instruction[Int], instance: T, val thisDatastore: Datastore) extends Instruction[T] {
    override def result = {
      instruction.result
      instance
    }
  }

  class PersistInsertInstruction[T](insert: InsertSingle, caseValue: CaseValue, instance: T) extends Instruction[T] {
    override protected def thisDatastore = insert.values.head.column.table.datastore

    override def result = {
      val id = insert.result
      caseValue.copy[T](instance, id)
    }
  }
}