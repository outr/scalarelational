package org

import scala.reflect.ClassTag
import scala.language.implicitConversions

import org.scalarelational.op.Condition
import org.scalarelational.table.Table
import org.scalarelational.column.{ColumnValue, Column}
import org.scalarelational.instruction.{Update, InsertSingle}

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object scalarelational {
  implicit def t2ColumnValue[T](t: (Column[T], T)): ColumnValue[T] = ColumnValue[T](t._1, t._2, None)
  implicit def columnValue2Condition[T](cv: ColumnValue[T]): Condition = cv.column === cv.value

  /** Cannot be merged with [[Table]] because importing Datastore and the table
    * into the namespace would result in `insert` calls being ambiguous.
    */
  implicit class MappableTable[Mapped: ClassTag](table: Table[Mapped]) {
    def insert(value: Mapped, strictMapping: Boolean = true)
              (implicit manifest: Manifest[Mapped]): InsertSingle[Mapped] = {
      val values = mapper.Reflection.fieldValues(table, value, strictMapping)
      table.insertColumnValues(values)
    }

    def update(value: Mapped, strictMapping: Boolean = true)
              (implicit manifest: Manifest[Mapped]): Update[Mapped] = {
      val values = mapper.Reflection.fieldValues(table, value, strictMapping)
      table.updateColumnValues(values)
    }
  }
}