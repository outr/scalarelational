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
//    def macroTo[R1: WeakTypeTag, R2: WeakTypeTag](table1: Table, table2: Table) = query.convert[(R1, R2)](macroConverter[R1, R2](table1, table2))

//    def macroTo[R](table: Table)

//    def macroConverter[R](table: Table): ResultConverter[R] = macro Macros.converter1[R]
//    def macroConverter[R1, R2](table1: Table, table2: Table): ResultConverter[(R1, R2)] = macro Macros.converter2[R1, R2]

  def macroTo[R](table: Table): Query[Vector[SelectExpression[_]], R] =
    macro Macros.to[R]
  }
}
