package org.scalarelational.datatype

import java.sql.{Blob, Timestamp}

import org.powerscala.enum.EnumEntry
import org.scalarelational.WrappedString
import org.scalarelational.datatype.create.{EnumDataTypeCreator, OptionDataTypeCreator, RefDataTypeCreator}


trait DataTypeSupport {
  import DataTypes._

  implicit def bigDecimalType: SimpleDataType[BigDecimal] = BigDecimalType
  implicit def booleanType: SimpleDataType[Boolean] = BooleanType
  implicit def blobType: SimpleDataType[Blob] = BlobType
  implicit def byteArrayType: SimpleDataType[Array[Byte]] = ByteArrayType
  implicit def doubleType: SimpleDataType[Double] = DoubleType
  implicit def intType: SimpleDataType[Int] = IntType
  implicit def longType: SimpleDataType[Long] = LongType
  implicit def stringType: SimpleDataType[String] = StringType
  implicit def timestampType: SimpleDataType[Timestamp] = TimestampType
  implicit def wrappedStringType: SimpleDataType[WrappedString] = WrappedStringType

  implicit def longTimestampType: DataType[Long, Timestamp] = LongTimestampType

  implicit def option[T, S](implicit dataType: DataType[T, S]): DataType[Option[T], S] = {
    OptionDataTypeCreator.create[T, S](dataType)
  }
  implicit def reference[T]: DataType[Ref[T], Int] = RefDataTypeCreator.create[T]

  implicit def enum[T <: EnumEntry](implicit manifest: Manifest[T]): DataType[T, String] = {
    new EnumDataTypeCreator[T].create()
  }
}