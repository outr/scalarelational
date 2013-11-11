package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Join(table: Table, joinType: JoinType = JoinType.Join, conditions: List[Condition] = Nil)

// Used for DSL before the actual Join instance is created
case class PartialJoin(query: Query, table: Table, joinType: JoinType) {
  def on(condition: Condition) = query.copy(joins = (Join(table, joinType, List(condition)) :: query.joins.reverse).reverse)
}

class JoinType private() extends EnumEntry

object JoinType extends Enumerated[JoinType] {
  val Join = new JoinType
  val Left = new JoinType
  val LeftOuter = new JoinType
  val Inner = new JoinType
  val Outer = new JoinType
}