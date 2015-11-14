package org.scalarelational.instruction

import org.scalarelational.op.{Condition, Conditions}


trait WhereSupport[+S <: WhereSupport[S]] extends SQLStatement {
  def whereCondition: Condition

  def where(condition: Condition): S

  def and(condition: Condition) = if (whereCondition != null) {
    where(Conditions(List(whereCondition, condition), ConnectType.And))
  } else {
    where(condition)
  }
  def or(condition: Condition) = if (whereCondition != null) {
    where(Conditions(List(whereCondition, condition), ConnectType.Or))
  } else {
    where(condition)
  }
}