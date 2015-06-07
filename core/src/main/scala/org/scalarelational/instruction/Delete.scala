package org.scalarelational.instruction

import org.scalarelational.op.Condition
import org.scalarelational.model.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Delete(table: Table,
                  whereCondition: Condition = null) extends WhereSupport[Delete] with Instruction[Int] {
  def where(condition: Condition) = copy(whereCondition = condition)

  def result = {
    val datastore = table.datastore
    datastore.exec(this)
  }
  def async = table.datastore.async {
    result
  }
}