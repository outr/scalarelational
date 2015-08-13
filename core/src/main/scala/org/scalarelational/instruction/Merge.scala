package org.scalarelational.instruction

import org.scalarelational.column.{Column, ColumnValue}
import org.scalarelational.table.Table

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Merge(table: Table, key: Column[_, _], values: List[ColumnValue[_, _]])
  extends Instruction[Int] {
  def result: Int = table.datastore.exec(this)
}