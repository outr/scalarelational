package org.scalarelational.extra

import scala.reflect.macros._
import scala.language.experimental.macros
import scala.annotation.{compileTimeOnly, StaticAnnotation}

import org.scalarelational.table.Table

/**
 * Typed Table allows generation of a Table object automatically from a provided case class. You can set specific
 * properties or add additional fields to the object and they will be retained. This is all generated at compile-time
 * via Macros.
 *
 * @author Matt Hicks <matt@outr.com>
 */
@compileTimeOnly("Enable macro paradise to expand macro annotations")
class typedTable[T] extends StaticAnnotation {
  def macroTransform(annottees: Any*): Table = macro TypedTableGenerator.impl[T]
}

object TypedTableGenerator {
  def impl[T](c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Table] = {
    import c.universe._

    val q"""new typedTable[$cls]().macroTransform($a)""" = c.macroApplication
    val tpe = c.typecheck(q"(??? : $cls)").tpe
    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.get.paramLists.head
    val columns = fields.map(f => {
      val name = f.name.toTermName
      val decoded = name.decodedName.toString.toUpperCase
      val returnType = tpe.decl(name).typeSignature
      q"val $name = column[$returnType]($decoded)"
    })

    def modifiedObject(objectDef: ModuleDef): c.Expr[Table] = {
      val ModuleDef(_, objectName, template) = objectDef
      val body = template.body.tail     // Drop the init method
      val decoded = objectName.decodedName.toString.toUpperCase
      val ret = q"""
        object $objectName extends Table($decoded) {
          ..$columns
          ..$body
        }
      """
      c.Expr[Table](ret)
    }

    annottees.map(_.tree) match {
      case (objectDecl: ModuleDef) :: _ => modifiedObject(objectDecl)
      case x => c.abort(c.enclosingPosition, s"@table can only be applied to an object, not to $x")
    }
  }
}