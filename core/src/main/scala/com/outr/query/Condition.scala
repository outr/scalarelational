package com.outr.query

import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Condition {
  def and(condition: Condition) = Conditions(List(this, condition), ConnectType.And)
  def or(condition: Condition) = Conditions(List(this, condition), ConnectType.Or)
}

case class DirectCondition[T](column: Column[T], operator: Operator, value: T) extends Condition

case class RangeCondition[T](column: Column[T], operator: Operator, values: Seq[T]) extends Condition

case class LikeCondition[T](column: Column[T], regex: Regex) extends Condition

case class Conditions(list: List[Condition], connectType: ConnectType) {
  def and(condition: Condition) = if (connectType == ConnectType.And) {
    copy(list = (condition :: list.reverse).reverse)
  } else {
    throw new RuntimeException("Cannot add AND for conditions already connected with OR.")
  }

  def or(condition: Condition) = if (connectType == ConnectType.Or) {
    copy(list = (condition :: list.reverse).reverse)
  } else {
    throw new RuntimeException("Cannot add OR for conditions already connected with AND.")
  }
}