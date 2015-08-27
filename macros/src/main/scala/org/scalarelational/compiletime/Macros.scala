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

  def to1[R](c: blackbox.Context)
           (table: c.Expr[Table])
           (implicit r: c.WeakTypeTag[R]): c.Expr[Query[Vector[SelectExpression[_]], R]] = {
    import c.universe._

    val query = c.prefix.tree match {
      case Apply(_, List(qry)) => qry
    }

    val instance = typeWrapper[R](c)(weakTypeOf[R], table)

    val conv = q"""
       val converter = new org.scalarelational.instruction.ResultConverter[$r] {
         def apply(result: org.scalarelational.result.QueryResult[$r]): $r = {
           $instance
         }
       }
       $query.convert[$r](converter)
    """
    c.Expr[Query[Vector[SelectExpression[_]], R]](conv)
  }

  def to2[R1, R2](c: blackbox.Context)
            (table1: c.Expr[Table], table2: c.Expr[Table])
            (implicit r1: c.WeakTypeTag[R1], r2: c.WeakTypeTag[R2]): c.Expr[Query[Vector[SelectExpression[_]], (R1, R2)]] = {
    import c.universe._

    val query = c.prefix.tree match {
      case Apply(_, List(qry)) => qry
    }

    val instance1 = typeWrapper[R1](c)(weakTypeOf[R1], table1)
    val instance2 = typeWrapper[R2](c)(weakTypeOf[R2], table2)

    val conv = q"""
       val converter = new org.scalarelational.instruction.ResultConverter[($r1, $r2)] {
         def apply(result: org.scalarelational.result.QueryResult[($r1, $r2)]): ($r1, $r2) = {
           ($instance1, $instance2)
         }
       }
       $query.convert[($r1, $r2)](converter)
    """
    c.Expr[Query[Vector[SelectExpression[_]], (R1, R2)]](conv)
  }

  private def typeWrapper[T](c: blackbox.Context)(tpe: c.universe.Type, table: c.Expr[Table]) = {
    import c.universe._

    tpe.baseType(typeOf[Option[_]].typeSymbol) match {
      case TypeRef(_, _, targ :: Nil) =>
        q"""
           if (${hasAny[T](c)(targ, table)}) {
            Some(${type2Instance[T](c)(targ, table)})
           } else {
            None
           }
         """
      case NoType => type2Instance[T](c)(tpe, table)
    }
  }

  private def hasAny[T](c: blackbox.Context)(tpe: c.universe.Type, table: c.Expr[Table]) = {
    import c.universe._

    val members = tpe.decls
    val fields = members.filter(_.asTerm.isVal)
    val args = fields.map { field =>
      val name = TermName(simpleName(field.fullName))
      q"result.has($table.$name)"
    }
    q"List(..$args).find(b => b).nonEmpty"
  }

  private def type2Instance[T](c: blackbox.Context)(tpe: c.universe.Type, table: c.Expr[Table]) = {
    import c.universe._

    val members = tpe.decls
    val fields = members.filter(_.asTerm.isVal)
    val args = fields.map { field =>
      val name = TermName(simpleName(field.fullName))
      q"result($table.$name)"
    }
    val companion = tpe.typeSymbol.companion
    q"$companion(..$args)"
  }
}
