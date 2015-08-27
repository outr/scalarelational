package org.scalarelational

import org.scalarelational.instruction.{Query, ResultConverter}
import org.scalarelational.compiletime.{QueryMacros, Macros}
import org.scalarelational.table.Table

import scala.language.experimental.macros
import scala.reflect.runtime.universe._

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object mapper {
  implicit class MapperQuery[Expressions, Result](query: Query[Expressions, Result]) {
    def to[R](table: Table): Query[Vector[SelectExpression[_]], R] =
      macro QueryMacros.to1[R]

    def to[R1, R2](table1: Table, table2: Table): Query[Vector[SelectExpression[_]], (R1, R2)] =
      macro QueryMacros.to2[R1, R2]

    def to[R1, R2, R3](table1: Table, table2: Table, table3: Table): Query[Vector[SelectExpression[_]], (R1, R2, R3)] =
      macro QueryMacros.to3[R1, R2, R3]
  }
}