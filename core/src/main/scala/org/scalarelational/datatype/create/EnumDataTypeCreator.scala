package org.scalarelational.datatype.create

import java.sql.Types

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.powerscala.reflect._
import org.scalarelational.column.ColumnLike
import org.scalarelational.datatype.{DataType, SQLConversion, SQLType}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class EnumDataTypeCreator[T <: EnumEntry](implicit manifest: Manifest[T]) extends SQLConversion[T, String] {
  val enumerated = manifest.runtimeClass.instance
    .getOrElse(throw new RuntimeException(s"Unable to find companion for ${manifest.runtimeClass}"))
    .asInstanceOf[Enumerated[T]]

  val length = math.max(enumerated.values.maxBy(_.name.length).name.length, 128)

  def create() = new DataType[T, String](Types.VARCHAR, SQLType(s"VARCHAR($length)"), this)

  override def toSQL(column: ColumnLike[T, String], value: T): String = value.name
  override def fromSQL(column: ColumnLike[T, String], value: String): T = enumerated(value)
}