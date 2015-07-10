package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.column.property.AutoIncrement
import org.scalarelational.instruction.{InsertSingle, Instruction, Query}
import org.scalarelational.model.{Column, Table}
import org.scalarelational.result.QueryResult

import scala.util.Try
import scala.reflect.ClassTag
import scala.reflect.runtime.currentMirror

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
    def asCase[R](classForRow: QueryResult[R] => Class[_])(implicit manifest: Manifest[R]): Query[Expressions, R] = {
      query.convert[R] { r =>
        val clazz = classForRow(r)
        clazz.create[R](r.toFieldMap)
      }
    }
  }

  implicit class MappableTable(table: Table) {
    def simpleName(fullName: String) =
      fullName.lastIndexOf('.') match {
        case -1 => fullName
        case position => fullName.substring(position + 1)
      }

    private def fieldValues[T: ClassTag](value: T, strictMapping: Boolean): Seq[ColumnValue[Any]] = {
      val refl = currentMirror.reflect(value)
      val members = refl.symbol.asType.typeSignature.members
      val fields = members.filter(_.asTerm.isVal)

      fields.flatMap { field =>
        val setter = members
          .find(member => member.fullName == field.fullName && member.isMethod)
          .get.asMethod

        val f = simpleName(field.fullName)
        val column = table.getColumnByField[Any](f)

        if (column.isEmpty && strictMapping)
          throw new RuntimeException(s"Field $f has no corresponding column")
        else {
          val v = refl.reflectMethod(setter)(value)

          Try(column.map(c => c(v))).getOrElse {
            throw new RuntimeException(s"Field $f incompatible to table column type ${column.get.classType}")
          }
        }
      }.toSeq
    }

    def persist[T: ClassTag](value: T,
                             forceInsert: Boolean = false,
                             strictMapping: Boolean = true): Instruction[T] = {
      val primaryColumn = table.primaryKeys.head.asInstanceOf[Column[Any]]
      val values = fieldValues(value, strictMapping)
      val updates = values.filterNot(_.column == primaryColumn && primaryColumn.has(AutoIncrement))
      values.find(_.column == primaryColumn) match {
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
            new InstanceInstruction[T](update, value, table)
          } else {
            val clazz: EnhancedClass = value.getClass
            val primaryKeyCaseValue = clazz.caseValue(primaryColumn.fieldName).getOrElse(throw new RuntimeException(s"Unable to find case value for ${primaryColumn.name} (field name: ${primaryColumn.fieldName}) in $clazz."))
            new PersistInsertInstruction[T](table.datastore.insert(updates: _*), primaryKeyCaseValue, value)
          }
        }
        case None => new InstanceInstruction[T](table.datastore.insert(updates: _*), value, table)
      }
    }
  }

  class InstanceInstruction[T](instruction: Instruction[Int], instance: T, val table: Table) extends Instruction[T] {
    override def result = {
      instruction.result
      instance
    }
  }

  class PersistInsertInstruction[T](insert: InsertSingle, caseValue: CaseValue, instance: T) extends Instruction[T] {
    override def table = insert.values.head.column.table

    override def result = {
      val id = insert.result
      caseValue.copy[T](instance, id)
    }
  }
}