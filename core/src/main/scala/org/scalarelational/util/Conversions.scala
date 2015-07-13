package org.scalarelational.util

import scala.language.implicitConversions

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Conversions {
  implicit def tuple2ToList[T, T1 <: T, T2 <: T](t: (T1, T2)): List[T] = {
    List(t._1, t._2)
  }
  implicit def tuple3ToList[T, T1 <: T, T2 <: T, T3 <: T](t: (T1, T2, T3)): List[T] = {
    List(t._1, t._2, t._3)
  }
  implicit def tuple4ToList[T, T1 <: T, T2 <: T, T3 <: T, T4 <: T](t: (T1, T2, T3, T4)): List[T] = {
    List(t._1, t._2, t._3, t._4)
  }
  implicit def tuple5ToList[T, T1 <: T, T2 <: T, T3 <: T, T4 <: T, T5 <: T](t: (T1, T2, T3, T4, T5)): List[T] = {
    List(t._1, t._2, t._3, t._4, t._5)
  }
  implicit def tuple6ToList[T, T1 <: T, T2 <: T, T3 <: T, T4 <: T, T5 <: T, T6 <: T](t: (T1, T2, T3, T4, T5, T6)): List[T] = {
    List(t._1, t._2, t._3, t._4, t._5, t._6)
  }
}