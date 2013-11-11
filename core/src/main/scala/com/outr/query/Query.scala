package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query(expressions: List[SelectExpression],
                 table: Table = null,
                 joins: List[Join] = Nil,
                 whereCondition: Condition = null,
                 _limit: Int = -1,
                 _offset: Int = -1) extends WhereSupport[Query] {
  def from(table: Table) = copy(table = table)
  def where(condition: Condition) = copy(whereCondition = condition)

  def join(table: Table, joinType: JoinType = JoinType.Join) = PartialJoin(this, table, joinType)

  def innerJoin(table: Table) = join(table, joinType = JoinType.Inner)

  def limit(value: Int) = copy(_limit = value)
  def offset(value: Int) = copy(_offset = value)

  // TODO: group by
  // TODO: order by
}