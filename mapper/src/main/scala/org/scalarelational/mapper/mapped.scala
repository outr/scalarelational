package org.scalarelational.mapper

import scala.reflect.macros._
import scala.language.experimental.macros
import scala.annotation.compileTimeOnly

import org.scalarelational.table.Table
import org.scalarelational.column.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
@compileTimeOnly("Enable macro paradise to expand macro annotations")
object mapped {
  def simpleName(fullName: String) =
    fullName.lastIndexOf('.') match {
      case -1       => fullName
      case position => fullName.substring(position + 1)
    }

  def mapTo[T](c: blackbox.Context)
              (table: c.Expr[Table])
              (implicit t: c.WeakTypeTag[T]): c.Expr[List[ColumnValue[Any]]] = {
    import c.universe._

    val members = weakTypeOf[T].decls
    val fields  = members.filter(_.asTerm.isVal)

    val columns = fields.map { field =>
      // TODO field.name.toTermName contains a trailing space
      val name = TermName(simpleName(field.fullName))
      q"$table.$name($name)"
    }

    val columnTree = q"List(..$columns).asInstanceOf[List[ColumnValue[Any]]]"
    c.Expr[List[ColumnValue[Any]]](columnTree)
  }
}
