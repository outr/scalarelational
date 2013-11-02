package com.outr

import scala.language.implicitConversions

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object query {
  implicit def t2ColumnValue[T](t: (Column[T], T)) = ColumnValue[T](t._1, t._2)
}