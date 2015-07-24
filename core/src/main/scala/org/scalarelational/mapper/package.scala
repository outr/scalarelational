package org.scalarelational

import org.scalarelational.column.ColumnValue
import org.scalarelational.column.property.PrimaryKey
import org.scalarelational.instruction._
import org.scalarelational.table.Table

import scala.reflect.ClassTag
import scala.reflect.runtime.currentMirror
import scala.util.Try

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object mapper {
  implicit class MappableTable[Mapped: ClassTag](table: Table[Mapped]) {
    def simpleName(fullName: String) =
      fullName.lastIndexOf('.') match {
        case -1 => fullName
        case position => fullName.substring(position + 1)
      }

    private def fieldValues[T: ClassTag](value: T, strictMapping: Boolean): List[ColumnValue[Any]] = {
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
      }.toList
    }

    def insert(value: Mapped, strictMapping: Boolean = true): InsertSingle[Mapped] = {
      val values = fieldValues(value, strictMapping)
      insertColumnValues(table, values)
    }

    def update(value: Mapped, strictMapping: Boolean = true): Update[Mapped] = {
      val values = fieldValues(value, strictMapping)
      updateColumnValues(table, values)
    }
  }

  def updateColumnValues[T](table: Table[T], values: List[ColumnValue[Any]]): Update[T] = {
    val primaryKey = values.find(_.column.has(PrimaryKey))
      .getOrElse(throw new RuntimeException("Update must have a PrimaryKey value specified to be able to update."))
    val primaryColumn = primaryKey.column
    table.datastore.update(table, values: _*) where (primaryColumn === primaryKey.value)
  }

  def insertColumnValues[T](table: Table[T], values: List[ColumnValue[Any]]): InsertSingle[T] =
    table.datastore.insert(table, values: _*)
}
