package org.scalarelational.table

import org.scalarelational.column.Column
import org.scalarelational.column.types.ColumnType

import scala.language.implicitConversions

trait Table {
  implicit def columnType2Column[T](ct: ColumnType[T]): Column[T] = ???   // TODO: implement via Macro
}
