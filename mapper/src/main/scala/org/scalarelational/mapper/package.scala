package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.column.property.AutoIncrement
import org.scalarelational.instruction.{InsertSingle, Instruction, Query}
import org.scalarelational.model.{Datastore, Column, Table}
import org.scalarelational.result.QueryResult

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object mapper {
  implicit class MappableQuery[Expressions, OriginalResult](query: Query[Expressions, OriginalResult]) {
    def to[R](implicit manifest: Manifest[R]) = {
      val clazz: EnhancedClass = manifest.runtimeClass
      val f = (r: QueryResult[R]) => {
        clazz.create[R](r.toFieldMap)
      }
      query.convert[R](f)
    }
    def to[R1, R2](t1: Table, t2: Table)(implicit manifest1: Manifest[R1], manifest2: Manifest[R2]) = {
      val c1: EnhancedClass = manifest1.runtimeClass
      val c2: EnhancedClass = manifest2.runtimeClass
      val f = (r: QueryResult[(R1, R2)]) => {
        val r1 = c1.create[R1](r.toFieldMapForTable(t1))
        val r2 = c2.create[R2](r.toFieldMapForTable(t2))
        (r1, r2)
      }
      query.convert[(R1, R2)](f)
    }
    def to[R1, R2, R3](t1: Table, t2: Table, t3: Table)(implicit manifest1: Manifest[R1], manifest2: Manifest[R2], manifest3: Manifest[R3]) = {
      val c1: EnhancedClass = manifest1.runtimeClass
      val c2: EnhancedClass = manifest2.runtimeClass
      val c3: EnhancedClass = manifest3.runtimeClass
      val f = (r: QueryResult[(R1, R2, R3)]) => {
        val r1 = c1.create[R1](r.toFieldMapForTable(t1))
        val r2 = c2.create[R2](r.toFieldMapForTable(t2))
        val r3 = c3.create[R3](r.toFieldMapForTable(t3))
        (r1, r2, r3)
      }
      query.convert[(R1, R2, R3)](f)
    }
  }

  implicit class MappableTable(table: Table) {
    def persist[T <: AnyRef](value: T, forceInsert: Boolean = false)(implicit manifest: Manifest[T]): Instruction[T] = {
      val clazz: EnhancedClass = manifest.runtimeClass
      val primaryColumn = table.primaryKeys.head.asInstanceOf[Column[Any]]
      val values = clazz.caseValues.flatMap(cv => table.getColumnByField[Any](cv.name).map(c => c(cv[Any](value))))
      val updates = values.filterNot(cv => cv.column == primaryColumn && primaryColumn.has(AutoIncrement))
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
            val update = table.datastore.update(updates: _*) where (primaryColumn === primaryKey.value)
            new InstanceInstruction[T](update, value, table.datastore)
          } else {
            val primaryKeyCaseValue = clazz.caseValue(primaryColumn.fieldName).getOrElse(throw new RuntimeException(s"Unable to find case value for ${primaryColumn.name} (field name: ${primaryColumn.fieldName}) in $clazz."))
            new PersistInsertInstruction[T](table.datastore.insert(updates: _*), primaryKeyCaseValue, value)
          }
        }
        case None => new InstanceInstruction[T](table.datastore.insert(updates: _*), value, table.datastore)
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