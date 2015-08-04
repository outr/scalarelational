package org.scalarelational.instruction

import scala.language.existentials

import org.scalarelational.table.Table
import org.scalarelational.column.{ColumnValue, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Merge(table: Table, key: Column[_], values: List[ColumnValue[_]])
  extends Instruction[Int] {
  def result: Int = table.datastore.exec(this)
}