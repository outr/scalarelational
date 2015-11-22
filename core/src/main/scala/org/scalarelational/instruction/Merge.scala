package org.scalarelational.instruction

import org.scalarelational.Session
import org.scalarelational.column.{Column, ColumnValue}
import org.scalarelational.table.Table

import scala.language.existentials


case class Merge(table: Table, key: Column[_, _], values: List[ColumnValue[_, _]])
  extends Instruction[Int] {
  def result(implicit session: Session): Int = table.datastore.exec(this)
}