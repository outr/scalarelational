package org.scalarelational.datatype.create

import java.sql.Types

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.powerscala.reflect._
import org.scalarelational.column.ColumnLike
import org.scalarelational.datatype.{DataType, SQLConversion, SQLType}


class EnumDataTypeCreator[T <: EnumEntry](implicit manifest: Manifest[T]) extends SQLConversion[T, String] {
  val enumerated = manifest.runtimeClass.instance
    .getOrElse(throw new RuntimeException(s"Unable to find companion for ${manifest.runtimeClass}"))
    .asInstanceOf[Enumerated[T]]

  val length: Int = math.max(enumerated.values.maxBy(_.name.length).name.length, EnumDataTypeCreator.MaxLength)

  def create(): DataType[T, String] = new DataType[T, String](Types.VARCHAR, SQLType(s"VARCHAR($length)"), this)

  override def toSQL(column: ColumnLike[T, String], value: T): String = value.name
  override def fromSQL(column: ColumnLike[T, String], value: String): T = enumerated(value)
}

object EnumDataTypeCreator {
  val MaxLength = 128
}