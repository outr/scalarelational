package org.scalarelational.instruction

import org.scalarelational.op.{Condition, Conditions}


trait WhereSupport[+S <: WhereSupport[S]] extends SQLStatement {
  def whereCondition: Option[Condition]

  def where(condition: Condition): S

  def where(condition: Condition, connectType: ConnectType): S = whereCondition match {
    case Some(wc) => where(Conditions(List(wc, condition), connectType))
    case None => where(condition)
  }

  def and(condition: Condition): S = where(condition, ConnectType.And)
  def or(condition: Condition): S = where(condition, ConnectType.Or)
}