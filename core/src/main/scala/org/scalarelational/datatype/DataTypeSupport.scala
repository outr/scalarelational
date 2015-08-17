package org.scalarelational.datatype

import org.powerscala.enum.EnumEntry
import org.scalarelational.datatype.create.{EnumDataTypeCreator, OptionDataTypeCreator, RefDataTypeCreator}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DataTypeSupport {
  import DataTypes._

  implicit def bigDecimalType = BigDecimalType
  implicit def booleanType = BooleanType
  implicit def blobType = BlobType
  implicit def byteArrayType = ByteArrayType
  implicit def doubleType = DoubleType
  implicit def intType = IntType
  implicit def longType = LongType
  implicit def stringType = StringType
  implicit def timestampType = TimestampType
  implicit def wrappedStringType = WrappedStringType

  implicit def longTimestampType = LongTimestampType

  implicit def option[T, S](implicit dataType: DataType[T, S]) = OptionDataTypeCreator.create[T, S](dataType)
  implicit def reference[T] = RefDataTypeCreator.create[T]

  implicit def enum[T <: EnumEntry](implicit manifest: Manifest[T]) = new EnumDataTypeCreator[T].create()
}