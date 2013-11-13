package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query(expressions: List[SelectExpression],
                 table: Table = null,
                 joins: List[Join] = Nil,
                 whereCondition: Condition = null,
                 _groupBy: List[SelectExpression] = Nil,
                 _orderBy: List[OrderBy] = Nil,
                 _limit: Int = -1,
                 _offset: Int = -1) extends WhereSupport[Query] {
  def fields(expressions: SelectExpression*) = copy(expressions = this.expressions ::: expressions.toList)
  def fields(expressions: List[SelectExpression]) = copy(expressions = this.expressions ::: expressions)
  def clearFields() = copy(expressions = Nil)
  def from(table: Table) = copy(table = table)
  def where(condition: Condition) = copy(whereCondition = condition)

  def join(table: Table, joinType: JoinType = JoinType.Join, alias: String = null) = PartialJoin(this, table, joinType, alias)

  def innerJoin(table: Table) = join(table, joinType = JoinType.Inner)
  def innerJoin(alias: TableAlias) = join(alias.table, joinType = JoinType.Inner, alias = alias.tableAlias)

  def limit(value: Int) = copy(_limit = value)
  def offset(value: Int) = copy(_offset = value)

  def groupBy(expressions: SelectExpression*) = copy(_groupBy = _groupBy ::: expressions.toList)
  def orderBy(ordering: OrderBy*) = copy(_orderBy = _orderBy ::: ordering.toList)
}