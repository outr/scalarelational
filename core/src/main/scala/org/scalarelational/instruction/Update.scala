package org.scalarelational.instruction

import org.scalarelational.op.Condition
import org.scalarelational.table.Table
import org.scalarelational.column.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Update[T](table: Table[T],
                     values: List[ColumnValue[_]],
                     whereCondition: Condition = null
                    ) extends WhereSupport[Update[T]] with Instruction[T, Int] {
  def where(condition: Condition) = copy(whereCondition = condition)

  def result: Int = table.datastore.exec(this)

  /**
   * Returns a new copy of this Update with an additional column value. Will
   * replace if the column is already represented.
   */
  def add(value: ColumnValue[_]): Update[T] = {
    val filtered = values.filterNot(_.column == value.column)
    copy(values = value :: filtered.toList)
  }

  override def toString = s"Update(values = ${values.mkString(", ")}, table = ${table.tableName}, where = $whereCondition)"
}