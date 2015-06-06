package org.scalarelational

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Delete(table: Table,
                  whereCondition: Condition = null) extends WhereSupport[Delete] {
  def where(condition: Condition) = copy(whereCondition = condition)
}