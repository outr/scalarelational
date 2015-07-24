package org.scalarelational.mapper

import scala.util.Try
import scala.reflect.ClassTag
import scala.reflect.runtime.currentMirror

import org.scalarelational.table.Table
import org.scalarelational.column.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Reflection {
  def simpleName(fullName: String) =
    fullName.lastIndexOf('.') match {
      case -1 => fullName
      case position => fullName.substring(position + 1)
    }

  def fieldValues[T: ClassTag](table: Table[_], value: T, strictMapping: Boolean): List[ColumnValue[Any]] = {
    val refl = currentMirror.reflect(value)
    val members = refl.symbol.asType.typeSignature.members
    val fields = members.filter(_.asTerm.isVal)

    fields.flatMap { field =>
      val setter = members
        .find(member => member.fullName == field.fullName && member.isMethod)
        .get.asMethod

      val f = simpleName(field.fullName)
      val column = table.getColumnByField[Any](f)

      if (column.isEmpty && strictMapping)
        throw new RuntimeException(s"Field $f has no corresponding column")
      else {
        val v = refl.reflectMethod(setter)(value)

        Try(column.map(c => c(v))).getOrElse {
          throw new RuntimeException(s"Field $f incompatible to table column type ${column.get.classType}")
        }
      }
    }.toList
  }
}
