package org.scalarelational.datatype.create

import java.sql.Types

import enumeratum._
import org.scalarelational.column.ColumnLike
import org.scalarelational.datatype.{DataType, SQLConversion, SQLType}

class EnumDataTypeCreator[T <: EnumEntry](implicit enumerated: Enum[T], manifest: Manifest[T]) extends SQLConversion[T, String] {
  val length: Int = math.max(enumerated.values.maxBy(_.entryName.length).entryName.length, EnumDataTypeCreator.MaxLength)

  def create(): DataType[T, String] = new DataType[T, String](Types.VARCHAR, SQLType(s"VARCHAR($length)"), this)

  override def toSQL(value: T): String = value.entryName
  override def fromSQL(value: String): T = enumerated.withNameInsensitive(value)
}

object EnumDataTypeCreator {
  val MaxLength = 128
}