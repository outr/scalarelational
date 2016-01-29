package org.scalarelational.table

import org.scalarelational.column.Column
import org.scalarelational.column.types.ColumnType

import scala.language.implicitConversions

/**
  * Table represents a database table but should never be directly instantiated, but instead created via the Databases'
  * `table` method.
  */
trait Table {
  /**
    * Creates a Column around the ColumnType with a blank `name` if not defined by the type that will be derived from
    * the field during Macro generation of the table.
    *
    * @param ct the column type of the column
    * @tparam T the Scala type returned by the database
    * @return Column[T]
    */
  implicit def columnType2Column[T](ct: ColumnType[T]): Column[T] = Column[T](ct.columnName.getOrElse(""), ct)
}
