package org.scalarelational.gen

import org.scalarelational.table.Table
import org.scalarelational.table.property.TableProperty

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

@compileTimeOnly("Enable macro paradise to expand macro annotations")
object TableGeneration {
  def tables(c: blackbox.Context): c.Expr[Vector[Table]] = {
    import c.universe._

    val members = c.prefix.actualType.decls
    val fields = members.filter(m => m.asTerm.isVal && m.asTerm.info.baseType(typeOf[Table].typeSymbol) != NoType)
    val tableNames = fields.map(f => TermName(simpleName(f.fullName)))

    c.Expr[Vector[Table]](q"Vector(..$tableNames)")
  }

  // TODO: see if we can re-add this
//  def tablesMap(c: blackbox.Context): c.Expr[Map[Table, String]] = {
//    import c.universe._
//
//    val members = c.prefix.actualType.decls
//    val fields = members.filter(m => m.asTerm.isVal && m.asTerm.info.baseType(typeOf[Table].typeSymbol) != NoType)
//    val tablesMapped = fields.map(f => q"${TermName(simpleName(f.fullName))} -> ${simpleName(f.fullName)}")
//
//    c.Expr[Map[Table, String]](q"Map(..$tablesMapped)")
//  }

  def create[T <: Table](c: blackbox.Context)
                        (name: c.Expr[String], props: c.Expr[TableProperty]*)
                        (implicit t: c.WeakTypeTag[T]): c.Expr[T] = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val members = tpe.decls
    val fields = members.filter(m => m.asTerm.isVal)
    val columnNames = fields.map(f => TermName(simpleName(f.fullName)))
    val columnsMapped = fields.map(f => q"${TermName(simpleName(f.fullName))} -> ${simpleName(f.fullName)}")

    val table = q"""
      import org.scalarelational.table.property._

      new $tpe {
        override def database = ${c.prefix}
        override val properties: Set[TableProperty] = Set(TableName($name), ..$props)
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
