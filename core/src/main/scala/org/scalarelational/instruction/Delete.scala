package org.scalarelational.instruction

import org.scalarelational.Session
import org.scalarelational.op.Condition
import org.scalarelational.table.Table


case class Delete(table: Table, whereCondition: Condition = null)
  extends WhereSupport[Delete] with Instruction[Int] {

  def where(condition: Condition) = copy(whereCondition = condition)
  def result(implicit session: Session) = table.datastore.exec(this)
}