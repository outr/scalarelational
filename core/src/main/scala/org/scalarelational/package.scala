package org

import scala.language.implicitConversions

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object scalarelational {
  implicit def t2ColumnValue[T](t: (Column[T], T)) = ColumnValue[T](t._1, t._2, None)
  implicit def columnValue2Condition[T](cv: ColumnValue[T]) = cv.column === cv.value
}