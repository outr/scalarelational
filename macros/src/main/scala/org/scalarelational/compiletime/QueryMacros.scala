package org.scalarelational.compiletime

import java.sql.ResultSet

import org.scalarelational.instruction.{Query, ResultConverter}
import org.scalarelational.table.Table

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

@compileTimeOnly("Enable macro paradise to expand macro annotations")
object QueryMacros {
  def resultSetToR[R](c: blackbox.Context)(implicit r: c.WeakTypeTag[R]): c.Expr[ResultSet => R] = {
    import c.universe._

    def resultSetGet(field: c.universe.Symbol, tpe: Option[c.universe.Type] = None): c.universe.Tree = {
      val name = Macros.simpleName(field.fullName)
      val fieldType = tpe.getOrElse(field.info)
      if (fieldType.baseType(typeOf[String].typeSymbol) != NoType) {
        q"resultSet.getString($name)"
      } else if (fieldType.baseType(typeOf[Int].typeSymbol) != NoType) {
        q"resultSet.getInt($name)"
      } else if (fieldType.baseType(typeOf[Long].typeSymbol) != NoType) {
        q"resultSet.getLong($name)"
      } else if (fieldType.baseType(typeOf[java.math.BigDecimal].typeSymbol) != NoType) {
        q"resultSet.getBigDecimal($name)"
      } else if (fieldType.baseType(typeOf[scala.math.BigDecimal].typeSymbol) != NoType) {
        q"BigDecimal(resultSet.getBigDecimal($name))"
      } else if (fieldType.baseType(typeOf[Option[_]].typeSymbol) != NoType) {
        val TypeRef(_, _, targ :: Nil) = fieldType.baseType(typeOf[Option[_]].typeSymbol)
        q"Option(${resultSetGet(field, Some(targ))})"
      } else if (fieldType.baseType(typeOf[java.sql.Timestamp].typeSymbol) != NoType) {
        q"resultSet.getTimestamp($name)"
      } else {
        c.abort(c.enclosingPosition, s"Unsupported ResultSet conversion type: $fieldType.")
      }
    }

    val tpe = weakTypeOf[R]
    val members = tpe.decls
    val fields = members.filter(s => s.asTerm.isVal && s.asTerm.isCaseAccessor)
    val args = fields.map(field => resultSetGet(field))
    val companion = tpe.typeSymbol.companion
    val function = q"""(resultSet: java.sql.ResultSet) => {
      $companion(..$args)
    }
    """
    c.Expr[ResultSet => R](function)
  }

  def converterLoose[R](c: blackbox.Context)(implicit r: c.WeakTypeTag[R]): c.Expr[ResultConverter[R]] = {
    import c.universe._

    val instance = type2InstanceByName[R](c)(weakTypeOf[R])
    val converter = q"""
       new org.scalarelational.instruction.ResultConverter[$r] {
         def apply(result: org.scalarelational.result.QueryResult): $r = {
           $instance
         }
       }
    """
    c.Expr[ResultConverter[R]](converter)
  }

  def loose[E, R](c: blackbox.Context)
               (implicit r: c.WeakTypeTag[R]): c.Expr[Query[E, R]] = {
    import c.universe._

    val query = c.prefix.tree match {
      case Apply(_, List(qry)) => qry
    }

    val converter = converterLoose[R](c)(r)
    val converted = q"""
       $query.convert[$r]($converter)
    """
    c.Expr[Query[E, R]](converted)
  }

  def converter1[R](c: blackbox.Context)
                   (table: c.Expr[Table])
                   (implicit r: c.WeakTypeTag[R]): c.Expr[ResultConverter[R]] = {
    import c.universe._

    val instance = typeWrapper[R](c)(weakTypeOf[R], table)
    val converter = q"""
       new org.scalarelational.instruction.ResultConverter[$r] {
         def apply(result: org.scalarelational.result.QueryResult): $r = {
           $instance
         }
       }
    """
    c.Expr[ResultConverter[R]](converter)
  }

  def to1[E, R](c: blackbox.Context)
            (table: c.Expr[Table])
            (implicit r: c.WeakTypeTag[R]): c.Expr[Query[E, R]] = {
    import c.universe._

    val query = c.prefix.tree match {
      case Apply(_, List(qry)) => qry
    }

    val converter = converter1[R](c)(table)(r)
    val converted = q"""
       $query.convert[$r]($converter)
    """
    c.Expr[Query[E, R]](converted)
  }

  def to2[E, R1, R2](c: blackbox.Context)
                 (table1: c.Expr[Table], table2: c.Expr[Table])
                 (implicit r1: c.WeakTypeTag[R1], r2: c.WeakTypeTag[R2]): c.Expr[Query[E, (R1, R2)]] = {
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
    c.Expr[Query[E, (R1, R2)]](conv)
  }

  def to3[E, R1, R2, R3](c: blackbox.Context)
                     (table1: c.Expr[Table], table2: c.Expr[Table], table3: c.Expr[Table])
                     (implicit r1: c.WeakTypeTag[R1], r2: c.WeakTypeTag[R2], r3: c.WeakTypeTag[R3]): c.Expr[Query[E, (R1, R2, R3)]] = {
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
    c.Expr[Query[E, (R1, R2, R3)]](conv)
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
    val fields = members.filter(s => s.asTerm.isVal && s.asTerm.isCaseAccessor)
    val args = fields.map { field =>
      val name = TermName(Macros.simpleName(field.fullName))
      q"result.has($table.$name)"
    }
    q"Set(..$args).contains(true)"
  }

  private def type2Instance[T](c: blackbox.Context)(tpe: c.universe.Type, table: c.Expr[Table]) = {
    import c.universe._

    val members = tpe.decls
    val fields = members.filter(s => s.asTerm.isVal && s.asTerm.isCaseAccessor)
    val args = fields.map { field =>
      val name = TermName(Macros.simpleName(field.fullName))
      q"result($table.$name)"
    }
    val companion = tpe.typeSymbol.companion
    q"$companion(..$args)"
  }

  private def type2InstanceByName[T](c: blackbox.Context)(tpe: c.universe.Type) = {
    import c.universe._

    val members = tpe.decls
    val fields = members.filter(s => s.asTerm.isVal && s.asTerm.isCaseAccessor)
    val args = fields.map { field =>
      val name = Macros.simpleName(field.fullName)
      val fieldType = field.info
      q"result.byName[$fieldType]($name)"
    }
    val companion = tpe.typeSymbol.companion
    q"$companion(..$args)"
  }
}
