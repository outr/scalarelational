package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.compiletime.QueryMacros
import org.scalarelational.instruction.Query
import org.scalarelational.result.QueryResult
import org.scalarelational.table.Table

import scala.language.experimental.macros


package object mapper {
  implicit class MapperQuery[Expressions, Result](query: Query[Expressions, Result]) {
    def to[R](implicit manifest: Manifest[R]): Query[Expressions, R] = {
      val clazz: EnhancedClass = manifest.runtimeClass
      val f = (r: QueryResult) => {
        clazz.create[R](r.toFieldMap)
      }
      query.convert[R](f)
    }

    def toMacro[R](table: Table): Query[Vector[SelectExpression[_]], R] =
      macro QueryMacros.to1[R]

    def to[R1, R2](table1: Table, table2: Table): Query[Vector[SelectExpression[_]], (R1, R2)] =
      macro QueryMacros.to2[R1, R2]

    def to[R1, R2, R3](table1: Table, table2: Table, table3: Table): Query[Vector[SelectExpression[_]], (R1, R2, R3)] =
      macro QueryMacros.to3[R1, R2, R3]
  }
}