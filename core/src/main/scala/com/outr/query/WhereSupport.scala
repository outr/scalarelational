package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait WhereSupport[S <: WhereSupport[S]] extends SQLStatement {
  def whereCondition: Condition

  def where(condition: Condition): S

  def and(condition: Condition) = where(Conditions(List(whereCondition, condition), ConnectType.And))
  def or(condition: Condition) = where(Conditions(List(whereCondition, condition), ConnectType.Or))
}