package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Group[T](val items: List[T])

case class Group1[T, T1 <: T](t1: T1) extends Group[T](List(t1))
case class Group2[T, T1 <: T, T2 <: T](t1: T1, t2: T2) extends Group[T](List(t1, t2))
case class Group3[T, T1 <: T, T2 <: T, T3 <: T](t1: T1, t2: T2, t3: T3) extends Group[T](List(t1, t2, t3))