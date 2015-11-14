package org.scalarelational.instruction.query

import org.scalarelational.instruction.{JoinType, Joinable, PartialJoin, Query}

trait JoinSupport[Types, Result] {
  this: Query[Types, Result] =>

  def join(joinable: Joinable, joinType: JoinType = JoinType.Join) = PartialJoin[Types, Result](this, joinable, joinType)
  def innerJoin(joinable: Joinable) = join(joinable, joinType = JoinType.Inner)
  def leftJoin(joinable: Joinable) = join(joinable, joinType = JoinType.Left)
  def leftOuterJoin(joinable: Joinable) = join(joinable, joinType = JoinType.LeftOuter)
}
