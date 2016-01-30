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
    val fields = members.filter(m => m.asTerm.isVal)
    val columnNames = fields.map(f => TermName(simpleName(f.fullName)))
    val columnsMapped = fields.map(f => q"${TermName(simpleName(f.fullName))} -> ${simpleName(f.fullName)}")

    val table = q"""
      new $tpe {
        override val columns: Vector[Column[_]] = Vector(..$columnNames)
        override protected val columnNameMap: Map[Column[_], String] = Map(..$columnsMapped)
      }
    """
    c.Expr[T](table)
  }

  def simpleName(fullName: String): String = fullName.lastIndexOf('.') match {
    case -1 => fullName
    case position => fullName.substring(position + 1)
  }
}
