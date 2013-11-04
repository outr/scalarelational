package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query(columns: List[Column[_]],
                 table: Table = null,
                 whereConditions: List[Condition] = Nil) {
  def from(table: Table) = copy(table = table)
  def where(condition: Condition) = copy(whereConditions = (condition :: whereConditions.reverse).reverse)
}