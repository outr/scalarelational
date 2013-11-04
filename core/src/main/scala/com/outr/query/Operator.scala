package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Operator private(val symbol: String) extends EnumEntry

object Operator extends Enumerated[Operator] {
  val Equal = new Operator("=")
  val NotEqual = new Operator("!=")
  val GreaterThan = new Operator(">")
  val LessThan = new Operator("<")
  val GreaterThanOrEqual = new Operator(">=")
  val LessThanOrEqual = new Operator("<=")
  val Between = new Operator("BETWEEN")
  val Like = new Operator("LIKE")
  val In = new Operator("IN")
}