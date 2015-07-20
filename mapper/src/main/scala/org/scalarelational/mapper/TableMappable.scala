package org.scalarelational.mapper

import org.scalarelational.ColumnValue
import org.scalarelational.model.property.column.property.PrimaryKey

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait TableMappable {
  def toColumnValues: List[ColumnValue[Any]]

  def persist = {
    val values = toColumnValues
    val table = values.head.column.table
    persistColumnValues(table, values)
  }

  def insert = {
    val values = toColumnValues
    val table = values.head.column.table
    insertColumnValues(table, values)
  }

  def update = {
    val values = toColumnValues
    val table = values.head.column.table
    val primaryKey = values.find(cv => cv.column.has(PrimaryKey)).getOrElse(throw new RuntimeException("Update must have a PrimaryKey value specified to be able to update."))
    updateColumnValues(table, primaryKey, values)
  }
}