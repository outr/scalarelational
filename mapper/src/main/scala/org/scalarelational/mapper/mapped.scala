package org.scalarelational.mapper

import org.scalarelational.model.Table

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros._

/**
 * @author Matt Hicks <matt@outr.com>
 */
@compileTimeOnly("enable macro paradise to expand macro annotations")
class mapped(table: Table) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro MappedMacro.impl
}

object MappedMacro {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def modified(classDecl: ClassDef): c.Expr[Any] = {
      val (className, fields, parents, body) = extractCaseClassesParts(classDecl)

      val params = fields.asInstanceOf[List[ValDef]] map {p => p.duplicate}

      val table = extractAnnotationParameters(c.prefix.tree).head
      val converted = params.map(p => {
        val name = p.name.toTermName
        q"$table.$name($name)"
      })

      c.Expr[Any](
        q"""
          case class $className(..$params) extends ..$parents with TableMappable {
            ..$body

            import org.scalarelational.ColumnValue
            def toColumnValues: List[ColumnValue[Any]] = {
              List(..$converted).asInstanceOf[List[ColumnValue[Any]]]
            }
          }
        """)
    }

    def extractCaseClassesParts(classDecl: ClassDef) = classDecl match {
      case q"case class $className(..$fields) extends ..$parents { ..$body }" => (className, fields, parents, body)
    }

    def extractAnnotationParameters(tree: Tree): List[c.universe.Tree] = tree match {
      case q"new $name( ..$params )" => params
      case _ => throw new Exception("ToStringObfuscate annotation must be have at least one parameter.")
    }

    annottees.map(_.tree) match {
      case (classDecl: ClassDef) :: _ => modified(classDecl)
      case x => c.abort(c.enclosingPosition, s"@mapped can only be applied to a case class, not to $x")
    }
  }
}