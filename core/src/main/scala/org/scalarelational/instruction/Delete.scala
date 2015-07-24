package org.scalarelational.instruction

import org.scalarelational.op.Condition
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Delete[T](table: Table[T], whereCondition: Condition = null)
  extends WhereSupport[Delete[T]] with Instruction[T, Int] {

  def where(condition: Condition) = copy(whereCondition = condition)
  def result = table.datastore.exec(this)
}