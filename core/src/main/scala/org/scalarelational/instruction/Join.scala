package org.scalarelational.instruction

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.scalarelational.model.Table
import org.scalarelational.op.Condition

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Join(table: Table, joinType: JoinType = JoinType.Join, condition: Condition, alias: String)

// Used for DSL before the actual Join instance is created
case class PartialJoin(query: Query, table: Table, joinType: JoinType, alias: String) {
  def as(alias: String) = copy(alias = alias)

  def on(condition: Condition) = query.copy(joins = (Join(table, joinType, condition, alias) :: query.joins.reverse).reverse)
}

sealed abstract class JoinType extends EnumEntry

object JoinType extends Enumerated[JoinType] {
  case object Join extends JoinType
  case object Left extends JoinType
  case object LeftOuter extends JoinType
  case object Inner extends JoinType
  case object Outer extends JoinType

  val values = findValues.toVector
}