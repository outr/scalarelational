package org.scalarelational.dsl

trait Instruction {
  def exec(): Int
}
