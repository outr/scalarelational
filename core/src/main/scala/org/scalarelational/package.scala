package org

import scala.language.implicitConversions

import org.scalarelational.op.Condition
import org.scalarelational.column.{ColumnValue, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object scalarelational {
  implicit def t2ColumnValue[T](t: (Column[T], T)): ColumnValue[T] = ColumnValue[T](t._1, t._2, None)
  implicit def columnValue2Condition[T](cv: ColumnValue[T]): Condition = cv.column === cv.value
}