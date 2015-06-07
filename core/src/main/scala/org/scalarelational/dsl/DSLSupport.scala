package org.scalarelational.dsl

import org.scalarelational.model.{Column, Table}
import org.scalarelational.{ColumnValue, SelectExpression}
import org.scalarelational.instruction._

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DSLSupport {
  def select(expressions: SelectExpression*) = Query(expressions.toList)
  def select(expressions: List[SelectExpression]) = Query(expressions)
  def insert(values: ColumnValue[_]*) = InsertSingle(values)
  def insertInto(table: Table, values: Any*) = insert(values.zip(table.columns).map {
    case (value, column) => column.asInstanceOf[Column[Any]](value)
  }: _*)
  def merge(key: Column[_], values: ColumnValue[_]*) = Merge(key, values.toList)
  def update(values: ColumnValue[_]*) = Update(values.toList, values.head.column.table)
  def delete(table: Table) = Delete(table)
}

object DSLSupport extends DSLSupport