package org.scalarelational.instruction

import scala.language.existentials

import org.scalarelational.column.{ColumnValue, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Merge(key: Column[_], values: List[ColumnValue[_]]) extends Instruction[Int] {
  override def table = key.table

  def result = {
    val datastore = key.table.datastore
    datastore.exec(this)
  }
}