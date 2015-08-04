package org.scalarelational.instruction

import org.scalarelational.op.Condition
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Delete(table: Table, whereCondition: Condition = null)
  extends WhereSupport[Delete] with Instruction[Int] {

  def where(condition: Condition) = copy(whereCondition = condition)
  def result = table.datastore.exec(this)
}