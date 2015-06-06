package org.scalarelational.instruction

import org.scalarelational.ColumnValue
import org.scalarelational.model.Column

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Merge(key: Column[_], values: List[ColumnValue[_]]) {
  def result = {
    val datastore = key.table.datastore
    datastore.exec(this)
  }
  def async = key.table.datastore.async {
    result
  }
}