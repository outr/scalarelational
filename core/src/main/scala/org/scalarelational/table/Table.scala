package org.scalarelational.table

import org.scalarelational.column.Column
import org.scalarelational.column.types.ColumnType

import scala.language.implicitConversions

/**
  * Table represents a database table but should never be directly instantiated, but instead created via the Databases'
  * `table` method.
  */
trait Table {
  def columns: Vector[Column[_]]
  protected def columnNameMap: Map[Column[_], String]

  /**
    * Creates a Column around the ColumnType for this table.
    *
    * @param ct the column type of the column
    * @tparam T the Scala type returned by the database
    * @return Column[T]
    */
  implicit def columnType2Column[T](ct: ColumnType[T]): Column[T] = Column[T](ct)(this)

  /**
    * Get the column name for the supplied column within this table.
    */
  def columnName[T](column: Column[T]): String = column.columnType.columnName.getOrElse(columnNameMap(column))
}
