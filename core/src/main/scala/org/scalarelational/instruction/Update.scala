package org.scalarelational.instruction

import org.scalarelational.op.Condition
import org.scalarelational.model.{Datastore, Table}
import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Update(values: List[ColumnValue[_]],
                  table: Table,
                  whereCondition: Condition = null) extends WhereSupport[Update] with Instruction[Int] {
  def where(condition: Condition) = copy(whereCondition = condition)

  def result = {
    val datastore = table.datastore
    datastore.exec(this)
  }

  /**
   * Returns a new copy of this Update with an additional column value. Will
   * replace if the column is already represented.
   */
  def add(value: ColumnValue[_]): Update = {
    val filtered = values.filterNot(_.column == value.column)
    copy(values = value :: filtered.toList)
  }

  override def toString = s"Update(values = ${values.mkString(", ")}, table = ${table.tableName}, where = $whereCondition)"
}