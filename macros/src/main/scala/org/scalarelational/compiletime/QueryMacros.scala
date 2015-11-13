package org.scalarelational.compiletime

import org.scalarelational.SelectExpression
import org.scalarelational.instruction.Query
import org.scalarelational.table.Table

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

/**
 * @author Matt Hicks <matt@outr.com>
 */
@compileTimeOnly("Enable macro paradise to expand macro annotations")
object QueryMacros {
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
         def apply(result: org.scalarelational.result.QueryResult): $r = {
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
         def apply(result: org.scalarelational.result.QueryResult): ($r1, $r2) = {
           ($instance1, $instance2)
         }
       }
       $query.convert[($r1, $r2)](converter)
    """
    c.Expr[Query[Vector[SelectExpression[_]], (R1, R2)]](conv)
  }

  def to3[R1, R2, R3](c: blackbox.Context)
                     (table1: c.Expr[Table], table2: c.Expr[Table], table3: c.Expr[Table])
                     (implicit r1: c.WeakTypeTag[R1], r2: c.WeakTypeTag[R2], r3: c.WeakTypeTag[R3]): c.Expr[Query[Vector[SelectExpression[_]], (R1, R2, R3)]] = {
    import c.universe._

    val query = c.prefix.tree match {
      case Apply(_, List(qry)) => qry
    }

    val instance1 = typeWrapper[R1](c)(weakTypeOf[R1], table1)
    val instance2 = typeWrapper[R2](c)(weakTypeOf[R2], table2)
    val instance3 = typeWrapper[R3](c)(weakTypeOf[R3], table3)

    val conv = q"""
       val converter = new org.scalarelational.instruction.ResultConverter[($r1, $r2, $r3)] {
         def apply(result: org.scalarelational.result.QueryResult): ($r1, $r2, $r3) = {
           ($instance1, $instance2, $instance3)
         }
       }
       $query.convert[($r1, $r2, $r3)](converter)
    """
    c.Expr[Query[Vector[SelectExpression[_]], (R1, R2, R3)]](conv)
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
      val name = TermName(Macros.simpleName(field.fullName))
      q"result.has($table.$name)"
    }
    q"Set(..$args).contains(true)"
  }

  private def type2Instance[T](c: blackbox.Context)(tpe: c.universe.Type, table: c.Expr[Table]) = {
    import c.universe._

    val members = tpe.decls
    val fields = members.filter(_.asTerm.isVal)
    val args = fields.map { field =>
      val name = TermName(Macros.simpleName(field.fullName))
      q"result($table.$name)"
    }
    val companion = tpe.typeSymbol.companion
    q"$companion(..$args)"
  }
}
