package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait WhereSupport[S <: WhereSupport[S]] extends SQLStatement {
  def whereCondition: Condition

  def where(condition: Condition): S

  def and(condition: Condition) = where(whereCondition and condition)
  def or(condition: Condition) = where(whereCondition or condition)
}