package org.scalarelational.instruction

import org.scalarelational.Session
import org.scalarelational.op.Condition
import org.scalarelational.table.Table


case class Delete(table: Table, whereCondition: Option[Condition] = None)
  extends WhereSupport[Delete] with Instruction[Int] {

  def where(condition: Condition): Delete = copy(whereCondition = Option(condition))
  def result(implicit session: Session): Int = table.database.exec(this)
}