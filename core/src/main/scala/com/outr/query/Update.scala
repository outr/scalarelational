package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Update(values: List[ColumnValue[_]],
                  table: Table,
                  whereBlock: WhereBlock = null) {
  def where(whereBlock: WhereBlock, connectType: ConnectType = ConnectType.And): Update = this.whereBlock match {
    case null => copy(whereBlock = whereBlock)
    case single: SingleWhereBlock => copy(whereBlock = MultiWhereBlock(List(single, whereBlock), connectType))
    case multiple: MultiWhereBlock => if (multiple.connectType != connectType) {
      throw new RuntimeException(s"Where type is ${multiple.connectType} and trying to add an additional with '$connectType'.")
    } else {
      copy(whereBlock = multiple.copy(blocks = (whereBlock :: multiple.blocks.reverse).reverse))
    }
  }
  def where(condition: Condition): Update = where(SingleWhereBlock(condition))
  def where(conditions: Conditions): Update = where(MultiWhereBlock(conditions.list.map(c => SingleWhereBlock(c)), conditions.connectType))
  def and(condition: Condition): Update = where(SingleWhereBlock(condition), ConnectType.And)
  def or(condition: Condition): Update = where(SingleWhereBlock(condition), ConnectType.Or)
  // TODO: cleanup the where clauses functionality - perhaps just have Conditionals?
}