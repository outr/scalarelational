package org.scalarelational.model2

import org.powerscala.enum.{Enumerated, EnumEntry}
import org.scalarelational.model2.query.Query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Join(joinable: Joinable, joinType: JoinType, condition: Condition) extends ModelEntry {
  override def toSQL = {
    val joinableSQL = joinable.toSQL
    val conditionSQL = condition.toSQL
    SQL(s"${joinType.value} ${joinableSQL.text} ON ${conditionSQL.text}", joinableSQL.args ::: conditionSQL.args)
  }
}

// Used for DSL before the actual Join instance is created
case class PartialJoin[G <: Group[Field[_]]](query: Query[G], joinable: Joinable, joinType: JoinType) {
  def on(condition: Condition) = query.copy[G](joins = (Join(joinable, joinType, condition) :: query.joins.reverse).reverse)
}

sealed abstract class JoinType(val value: String) extends EnumEntry

object JoinType extends Enumerated[JoinType] {
  case object Join extends JoinType("JOIN")
  case object Left extends JoinType("LEFT JOIN")
  case object LeftOuter extends JoinType("LEFT OUTER JOIN")
  case object Inner extends JoinType("INNER JOIN")
  case object Outer extends JoinType("OUTER JOIN")

  val values = findValues.toVector
}