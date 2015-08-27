package org.scalarelational

import org.scalarelational.instruction.{Query, ResultConverter}
import org.scalarelational.compiletime.Macros
import org.scalarelational.table.Table

import scala.language.experimental.macros
import scala.reflect.runtime.universe._

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object mapper {
  implicit class MapperQuery[Expressions, Result](query: Query[Expressions, Result]) {
    def macroTo[R](table: Table): Query[Vector[SelectExpression[_]], R] =
      macro Macros.to1[R]

    def macroTo[R1, R2](table1: Table, table2: Table): Query[Vector[SelectExpression[_]], (R1, R2)] =
      macro Macros.to2[R1, R2]
  }
}