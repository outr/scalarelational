package org

import org.scalarelational.column.{Column, ColumnValue}
import org.scalarelational.op.Condition

import scala.language.implicitConversions

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object scalarelational {
  implicit def t2ColumnValue[T, S](t: (Column[T, S], T)): ColumnValue[T, S] = ColumnValue[T, S](t._1, t._2, None)
  implicit def columnValue2Condition[T, S](cv: ColumnValue[T, S]): Condition = cv.column === cv.value
}