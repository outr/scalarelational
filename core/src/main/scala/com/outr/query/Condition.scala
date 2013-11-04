package com.outr.query

import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Condition

case class DirectCondition[T](column: Column[T], operator: Operator, value: T) extends Condition

case class RangeCondition[T](column: Column[T], operator: Operator, values: Seq[T]) extends Condition

case class LikeCondition[T](column: Column[T], regex: Regex) extends Condition