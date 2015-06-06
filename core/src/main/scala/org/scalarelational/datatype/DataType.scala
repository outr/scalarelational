package org.scalarelational.datatype

import java.sql.{Blob, Timestamp}

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.powerscala.reflect._
import org.scalarelational.column.WrappedString
import org.scalarelational.column.property.{IgnoreCase, NumericStorage}
import org.scalarelational.{Column, ColumnLike}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DataType[T] {
  def sqlType(column: ColumnLike[T]): String

  def toSQLType(column: ColumnLike[T], value: T): Any

  def fromSQLType(column: ColumnLike[T], value: Any): T
}

object BooleanDataType extends DataType[Boolean] {
  def sqlType(column: ColumnLike[Boolean]) = "BOOLEAN"
  def toSQLType(column: ColumnLike[Boolean], value: Boolean) = value
  def fromSQLType(column: ColumnLike[Boolean], value: Any) = value.asInstanceOf[Boolean]
}

object IntDataType extends DataType[Int] {
  def sqlType(column: ColumnLike[Int]) = "INTEGER"
  def toSQLType(column: ColumnLike[Int], value: Int) = value
  def fromSQLType(column: ColumnLike[Int], value: Any) = value.asInstanceOf[Int]
}

object JavaIntDataType extends DataType[java.lang.Integer] {
  def sqlType(column: ColumnLike[java.lang.Integer]) = "INTEGER"
  def toSQLType(column: ColumnLike[java.lang.Integer], value: java.lang.Integer) = value
  def fromSQLType(column: ColumnLike[java.lang.Integer], value: Any) = value.asInstanceOf[java.lang.Integer]
}

object LongDataType extends DataType[Long] {
  def sqlType(column: ColumnLike[Long]) = "BIGINT"
  def toSQLType(column: ColumnLike[Long], value: Long) = value
  def fromSQLType(column: ColumnLike[Long], value: Any) = value.asInstanceOf[Long]
}

object JavaLongDataType extends DataType[java.lang.Long] {
  def sqlType(column: ColumnLike[java.lang.Long]) = "BIGINT"
  def toSQLType(column: ColumnLike[java.lang.Long], value: java.lang.Long) = value
  def fromSQLType(column: ColumnLike[java.lang.Long], value: Any) = value.asInstanceOf[java.lang.Long]
}

object DoubleDataType extends DataType[Double] {
  def sqlType(column: ColumnLike[Double]) = "DOUBLE"
  def toSQLType(column: ColumnLike[Double], value: Double) = value
  def fromSQLType(column: ColumnLike[Double], value: Any) = value.asInstanceOf[Double]
}

object JavaDoubleDataType extends DataType[java.lang.Double] {
  def sqlType(column: ColumnLike[java.lang.Double]) = "DOUBLE"
  def toSQLType(column: ColumnLike[java.lang.Double], value: java.lang.Double) = value
  def fromSQLType(column: ColumnLike[java.lang.Double], value: Any) = value.asInstanceOf[java.lang.Double]
}

object BigDecimalDataType extends DataType[BigDecimal] {
  def sqlType(column: ColumnLike[BigDecimal]) = {
    val numericStorage = column match {
      case c: Column[BigDecimal] => c.get[NumericStorage](NumericStorage.Name).getOrElse(NumericStorage.DefaultBigDecimal)
      case _ => NumericStorage.DefaultBigDecimal
    }
    s"DECIMAL(${numericStorage.precision}, ${numericStorage.scale})"
  }
  def toSQLType(column: ColumnLike[BigDecimal], value: BigDecimal) = value.underlying()
  def fromSQLType(column: ColumnLike[BigDecimal], value: Any) = BigDecimal(value.asInstanceOf[java.math.BigDecimal])
}

object StringDataType extends DataType[String] {
  val VarcharType = s"VARCHAR(${Int.MaxValue})"
  val VarcharIngoreCaseType = s"VARCHAR_IGNORECASE(${Int.MaxValue})"

  def sqlType(column: ColumnLike[String]) = column match {
    case c: Column[_] if c.has(IgnoreCase) => VarcharIngoreCaseType
    case _ => VarcharType
  }
  def toSQLType(column: ColumnLike[String], value: String) = value
  def fromSQLType(column: ColumnLike[String], value: Any) = value.asInstanceOf[String]
}

object WrappedStringDataType extends DataType[WrappedString] {
  def sqlType(column: ColumnLike[WrappedString]) = column match {
    case c: Column[_] if c.has(IgnoreCase) => StringDataType.VarcharIngoreCaseType
    case _ => StringDataType.VarcharType
  }
  def toSQLType(column: ColumnLike[WrappedString], value: WrappedString) = value.value
  def fromSQLType(column: ColumnLike[WrappedString], value: Any) = column.manifest.runtimeClass.create(Map("value" -> value.asInstanceOf[String]))
}

object ByteArrayDataType extends DataType[Array[Byte]] {
  def sqlType(column: ColumnLike[Array[Byte]]) = "BINARY(1000)"
  def toSQLType(column: ColumnLike[Array[Byte]], value: Array[Byte]) = value
  def fromSQLType(column: ColumnLike[Array[Byte]], value: Any) = value.asInstanceOf[Array[Byte]]
}

object BlobDataType extends DataType[Blob] {
  def sqlType(column: ColumnLike[Blob]) = "BLOB"
  def toSQLType(column: ColumnLike[Blob], value: Blob) = value
  def fromSQLType(column: ColumnLike[Blob], value: Any) = value.asInstanceOf[Blob]
}

object TimestampDataType extends DataType[Timestamp] {
  def sqlType(column: ColumnLike[Timestamp]) = "TIMESTAMP"
  def toSQLType(column: ColumnLike[Timestamp], value: Timestamp) = value
  def fromSQLType(column: ColumnLike[Timestamp], value: Any) = value.asInstanceOf[Timestamp]
}

class EnumDataType[T <: EnumEntry](implicit manifest: Manifest[T]) extends DataType[T] {
  val enumerated = manifest.runtimeClass.instance.getOrElse(throw new RuntimeException("Unable to find companion for ${manifest.runtimeClass}")).asInstanceOf[Enumerated[T]]

  def sqlType(column: ColumnLike[T]) = s"VARCHAR(${Int.MaxValue})"
  def toSQLType(column: ColumnLike[T], value: T) = value match {
    case null => null
    case _ => value.name
  }
  def fromSQLType(column: ColumnLike[T], value: Any) = value match {
    case null => null.asInstanceOf[T]
    case s: String => enumerated(s)
  }
}