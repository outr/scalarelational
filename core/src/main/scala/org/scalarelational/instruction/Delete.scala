package org.scalarelational.instruction

import org.scalarelational.op.Condition
import org.scalarelational.model.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Delete(table: Table,
                  whereCondition: Condition = null) extends WhereSupport[Delete] with Instruction[Int] {
  override protected def thisDatastore = table.datastore

  def where(condition: Condition) = copy(whereCondition = condition)

  def result = thisDatastore.exec(this)
}