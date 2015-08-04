package org.scalarelational

import scala.reflect.ClassTag
import scala.language.implicitConversions

import org.scalarelational.datatype.Ref
import org.scalarelational.instruction.{Update, InsertSingle}

package object mapper {
  /** Cannot be merged with [[MappedTable]] because importing Datastore and the table
    * into the namespace would result in `insert` calls being ambiguous.
    */
  implicit class MappableTable[Mapped: ClassTag](table: MappedTable[Mapped]) {
    @deprecated("Use Entity mix-in for case classes instead of MappableTable", "1.1.0")
    def insert(value: Mapped, strictMapping: Boolean = true)
              (implicit manifest: Manifest[Mapped]): InsertSingle[Ref[Mapped]] = {
      val values = mapper.Reflection.fieldValues(table, value, strictMapping)
      table.insertColumnValues(values)
    }

    @deprecated("Use Entity mix-in for case classes instead of MappableTable", "1.1.0")
    def update(value: Mapped, strictMapping: Boolean = true)
              (implicit manifest: Manifest[Mapped]): Update[Ref[Mapped]] = {
      val values = mapper.Reflection.fieldValues(table, value, strictMapping)
      table.updateColumnValues(values)
    }
  }
}
