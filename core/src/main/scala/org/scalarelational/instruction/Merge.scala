package org.scalarelational.instruction

import org.scalarelational.ColumnValue
import org.scalarelational.model.{ColumnLike, Column}

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Merge(key: ColumnLike[_], values: List[ColumnValue[_]]) extends Instruction[Int] {
  override def table = key.table

  def result = {
    val datastore = key.table.datastore
    datastore.exec(this)
  }
}