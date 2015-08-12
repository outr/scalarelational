package org.scalarelational.op

import org.scalarelational.column.ColumnLike
import org.scalarelational.instruction.ConnectType

import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Condition {
  def and(condition: Condition) = Conditions(List(this, condition), ConnectType.And)
  def or(condition: Condition) = Conditions(List(this, condition), ConnectType.Or)
}

case class ColumnCondition[T, S](column: ColumnLike[T, S], operator: Operator, other: ColumnLike[T, S]) extends Condition

case class DirectCondition[T, S](column: ColumnLike[T, S], operator: Operator, value: T) extends Condition

case class RangeCondition[T, S](column: ColumnLike[T, S], operator: Operator, values: Seq[T]) extends Condition

case class LikeCondition[T, S](column: ColumnLike[T, S], pattern: String, not: Boolean) extends Condition

case class RegexCondition[T, S](column: ColumnLike[T, S], regex: Regex, not: Boolean) extends Condition

case class Conditions(list: List[Condition], connectType: ConnectType = ConnectType.And) extends Condition {
  override def and(condition: Condition) = if (connectType == ConnectType.And) {
    copy(list = (condition :: list.reverse).reverse)
  } else {
    throw new RuntimeException("Cannot add AND for conditions already connected with OR.")
  }

  override def or(condition: Condition) = if (connectType == ConnectType.Or) {
    copy(list = (condition :: list.reverse).reverse)
  } else {
    throw new RuntimeException("Cannot add OR for conditions already connected with AND.")
  }
}