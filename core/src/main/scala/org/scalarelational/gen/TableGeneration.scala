package org.scalarelational.gen

import org.scalarelational.table.Table
import org.scalarelational.table.property.TableProperty

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

@compileTimeOnly("Enable macro paradise to expand macro annotations")
object TableGeneration {
  def create[T <: Table](c: blackbox.Context)
                        (props: c.Expr[TableProperty]*)
                        (implicit t: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val members = tpe.decls
    val fields = members.filter(m => m.asTerm.isAccessor)

    println(s"Type: $tpe")
    println(s"Fields: $fields")
    println(s"Fields: ${fields.map(m => s"$m (${m.asMethod.returnType})").mkString("\n")}")

    val table = q"""
      new $tpe {
        override val id: org.scalarelational.column.Column[Int] = new org.scalarelational.column.Column[Int]("id", null)
      }
    """

    c.Expr[T](table)
  }
}
