package org.scalarelational.instruction.query

import org.scalarelational.{SelectExpression => SE}

trait SelectExpressions[T] {
  def vector: Vector[SE[_]]
}

case class SingleExpression[T](e: SE[T]) extends SelectExpressions[T] {
  lazy val vector = Vector(e)
}

case class TwoExpressions[T1, T2](e1: SE[T1], e2: SE[T2]) extends SelectExpressions[(T1, T2)] {
  lazy val vector = Vector(e1, e2)
}

case class ThreeExpressions[T1, T2, T3](e1: SE[T1], e2: SE[T2], e3: SE[T3]) extends SelectExpressions[(T1, T2, T3)] {
  lazy val vector = Vector(e1, e2, e3)
}

case class FourExpressions[T1, T2, T3, T4](e1: SE[T1], e2: SE[T2], e3: SE[T3], e4: SE[T4]) extends SelectExpressions[(T1, T2, T3, T4)] {
  lazy val vector = Vector(e1, e2, e3, e4)
}

case class FiveExpressions[T1, T2, T3, T4, T5](e1: SE[T1], e2: SE[T2], e3: SE[T3], e4: SE[T4], e5: SE[T5]) extends SelectExpressions[(T1, T2, T3, T4, T5)] {
  lazy val vector = Vector(e1, e2, e3, e4, e5)
}

case class SixExpressions[T1, T2, T3, T4, T5, T6](e1: SE[T1], e2: SE[T2], e3: SE[T3], e4: SE[T4], e5: SE[T5], e6: SE[T6]) extends SelectExpressions[(T1, T2, T3, T4, T5, T6)] {
  lazy val vector = Vector(e1, e2, e3, e4, e5, e6)
}

case class SevenExpressions[T1, T2, T3, T4, T5, T6, T7](e1: SE[T1], e2: SE[T2], e3: SE[T3], e4: SE[T4], e5: SE[T5], e6: SE[T6], e7: SE[T7]) extends SelectExpressions[(T1, T2, T3, T4, T5, T6, T7)] {
  lazy val vector = Vector(e1, e2, e3, e4, e5, e6, e7)
}

case class EightExpressions[T1, T2, T3, T4, T5, T6, T7, T8](e1: SE[T1], e2: SE[T2], e3: SE[T3], e4: SE[T4], e5: SE[T5], e6: SE[T6], e7: SE[T7], e8: SE[T8]) extends SelectExpressions[(T1, T2, T3, T4, T5, T6, T7, T8)] {
  lazy val vector = Vector(e1, e2, e3, e4, e5, e6, e7, e8)
}

case class NineExpressions[T1, T2, T3, T4, T5, T6, T7, T8, T9](e1: SE[T1], e2: SE[T2], e3: SE[T3], e4: SE[T4], e5: SE[T5], e6: SE[T6], e7: SE[T7], e8: SE[T8], e9: SE[T9]) extends SelectExpressions[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] {
  lazy val vector = Vector(e1, e2, e3, e4, e5, e6, e7, e8, e9)
}

case class TenExpressions[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](e1: SE[T1], e2: SE[T2], e3: SE[T3], e4: SE[T4], e5: SE[T5], e6: SE[T6], e7: SE[T7], e8: SE[T8], e9: SE[T9], e10: SE[T10]) extends SelectExpressions[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] {
  lazy val vector = Vector(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)
}

case class VariableExpressions[T](vector: Vector[SE[_]]) extends SelectExpressions[T]