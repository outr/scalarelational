package org.scalarelational.mapper

import scala.reflect.macros._
import scala.language.experimental.macros
import scala.annotation.{compileTimeOnly, StaticAnnotation}

import org.scalarelational.model.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
@compileTimeOnly("Enable macro paradise to expand macro annotations")
class mapped(table: Table) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro MappedMacro.impl
}

object MappedMacro {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def modified(classDecl: ClassDef): c.Expr[Any] = {
      val (className, fields, originalParents, body) = extractCaseClassesParts(classDecl)

      val parents = originalParents.asInstanceOf[List[Ident]]
        .filterNot(_.toString() == "Entity")

      val params = fields.asInstanceOf[List[ValDef]].map(_.duplicate)
      val table = extractAnnotationArgument(c.prefix.tree)

      val converted = params.map(p => {
        val name = p.name.toTermName
        q"$table.$name($name)"
      })

      c.Expr[Any](
        q"""
          case class $className(..$params) extends ..$parents with Entity {
            ..$body

            import org.scalarelational.ColumnValue
            override def toColumnValues: List[ColumnValue[Any]] =
              List(..$converted).asInstanceOf[List[ColumnValue[Any]]]
          }
        """)
    }

    def extractCaseClassesParts(classDecl: ClassDef) = classDecl match {
      case q"case class $className(..$fields) extends ..$parents { ..$body }" => (className, fields, parents, body)
    }

    def extractAnnotationArgument(tree: Tree): c.universe.Tree = tree match {
      case q"new $name($param)" => param
      case _ => c.abort(c.enclosingPosition, "@mapped annotation must have one argument")
    }

    annottees.map(_.tree) match {
      case (classDecl: ClassDef) :: _ => modified(classDecl)
      case x => c.abort(c.enclosingPosition, s"@mapped can only be applied to a case class, not to $x")
    }
  }
}