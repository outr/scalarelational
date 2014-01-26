package com.outr.query

import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Condition {
  def and(condition: Condition) = Conditions(List(this, condition), ConnectType.And)
  def or(condition: Condition) = Conditions(List(this, condition), ConnectType.Or)
}

case class NullCondition[T](column: ColumnLike[T], operator: Operator) extends Condition

case class ColumnCondition[T](column: ColumnLike[T], operator: Operator, other: ColumnLike[T]) extends Condition

case class DirectCondition[T](column: ColumnLike[T], operator: Operator, value: T) extends Condition

case class RangeCondition[T](column: ColumnLike[T], operator: Operator, values: Seq[T]) extends Condition

case class LikeCondition[T](column: ColumnLike[T], regex: Regex) extends Condition

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