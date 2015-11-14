package org.scalarelational.compiletime

import org.scalarelational.column.ColumnValue
import org.scalarelational.table.Table

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox


@compileTimeOnly("Enable macro paradise to expand macro annotations")
object Macros {
  def simpleName(fullName: String) =
    fullName.lastIndexOf('.') match {
      case -1       => fullName
      case position => fullName.substring(position + 1)
    }

  def mapTo[T](c: blackbox.Context)
              (table: c.Expr[Table])
              (implicit t: c.WeakTypeTag[T]): c.Expr[List[ColumnValue[Any, Any]]] = {
    import c.universe._

    val members = weakTypeOf[T].decls
    val fields  = members.filter(_.asTerm.isVal)

    val columns = fields.map { field =>
      // TODO field.name.toTermName contains a trailing space
      val name = TermName(simpleName(field.fullName))
      q"$table.$name($name)"
    }

    val listTree = q"List(..$columns)"
    val listTreeCast =
      q"$listTree.asInstanceOf[List[org.scalarelational.column.ColumnValue[Any, Any]]]"

    c.Expr[List[ColumnValue[Any, Any]]](listTreeCast)
  }
}
