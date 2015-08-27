package org.scalarelational.compiletime

import org.scalarelational.SelectExpression
import org.scalarelational.column.ColumnValue
import org.scalarelational.instruction.{Query, ResultConverter}
import org.scalarelational.table.Table

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

import scala.reflect.runtime.universe._

/**
 * @author Matt Hicks <matt@outr.com>
 */
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

  def to[R](c: blackbox.Context)
           (table: c.Expr[Table])
           (implicit r: c.WeakTypeTag[R]): c.Expr[Query[Vector[SelectExpression[_]], R]] = {
    import c.universe._

    val query = c.prefix.tree

    val tpe = weakTypeOf[R]
    val members = tpe.decls
    val fields = members.filter(_.asTerm.isVal)
    val args = fields.map { field =>
      val name = TermName(simpleName(field.fullName))
      q"result($table.$name)"
    }
    val companion = tpe.typeSymbol.companion

    val conv = q"""
       val converter = new org.scalarelational.instruction.ResultConverter[$r] {
         def apply(result: org.scalarelational.result.QueryResult[$r]): $r = {
           $companion(..$args)
         }
       }
       $query.convert[$r](converter)
    """
    c.Expr[Query[Vector[SelectExpression[_]], R]](conv)
  }

  /*def converter2[R1, R2](c: blackbox.Context)
                       (table1: c.Expr[Table], table2: c.Expr[Table])
                       (implicit r1: c.WeakTypeTag[R1], r2: c.WeakTypeTag[R2]): c.Expr[ResultConverter[(R1, R2)]] = {
    import c.universe._

//    weakTypeOf[R1].baseType(typeOf[Option[_]].typeSymbol) match {
//      case TypeRef(_, _, targ :: Nil) => println(s"R1 Targ: $targ")
//      case NoType => println(s"R1 is not Optional! ${weakTypeOf[R1]}")
//    }
//    weakTypeOf[R2].baseType(typeOf[Option[_]].typeSymbol) match {
//      case TypeRef(_, _, targ :: Nil) => println(s"R2 Targ: $targ")
//      case NoType => println(s"R2 is not Optional! ${weakTypeOf[R2]}")
//    }
    println(s"R1: ${weakTypeOf[R1]}, R2: ${weakTypeOf[R2]}")

    c.abort(c.enclosingPosition, "So far so good!")

    val conv = q"""
       new org.scalarelational.instruction.ResultConverter[($r1, $r2)] {
         def apply(result: org.scalarelational.result.QueryResult[($r1, $r2)]) = {
           null.asInstanceOf[($r1, $r2)]
         }
       }
    """
    c.Expr[ResultConverter[(R1, R2)]](conv)
  }*/
}
