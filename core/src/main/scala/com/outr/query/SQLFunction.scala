package com.outr.query

import org.powerscala.enum.{Enumerated, EnumEntry}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLFunction extends SelectExpression

class FunctionType private() extends EnumEntry

object FunctionType extends Enumerated[FunctionType] {
  val Min = new FunctionType
}

case class SimpleFunction(functionType: FunctionType, column: Column[_]) extends SQLFunction