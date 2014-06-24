package com.outr.query.convert

import com.outr.query.{Column, ColumnLike}
import org.powerscala.enum.{Enumerated, EnumEntry}
import org.powerscala.reflect._
import java.sql.{Timestamp, Blob}
import com.outr.query.column.property.IgnoreCase
import com.outr.query.column.WrappedString

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnConverter[T] {
  def sqlType(column: ColumnLike[T]): String

  def toSQLType(column: ColumnLike[T], value: T): Any

  def fromSQLType(column: ColumnLike[T], value: Any): T
}

object BooleanConverter extends ColumnConverter[Boolean] {
  def sqlType(column: ColumnLike[Boolean]) = "BOOLEAN"
  def toSQLType(column: ColumnLike[Boolean], value: Boolean) = value
  def fromSQLType(column: ColumnLike[Boolean], value: Any) = value.asInstanceOf[Boolean]
}

object IntConverter extends ColumnConverter[Int] {
  def sqlType(column: ColumnLike[Int]) = "INTEGER"
  def toSQLType(column: ColumnLike[Int], value: Int) = value
  def fromSQLType(column: ColumnLike[Int], value: Any) = value.asInstanceOf[Int]
}

object LongConverter extends ColumnConverter[Long] {
  def sqlType(column: ColumnLike[Long]) = "BIGINT"
  def toSQLType(column: ColumnLike[Long], value: Long) = value
  def fromSQLType(column: ColumnLike[Long], value: Any) = value.asInstanceOf[Long]
}

object DoubleConverter extends ColumnConverter[Double] {
  def sqlType(column: ColumnLike[Double]) = "DOUBLE"
  def toSQLType(column: ColumnLike[Double], value: Double) = value
  def fromSQLType(column: ColumnLike[Double], value: Any) = value.asInstanceOf[Double]
}

object BigDecimalConverter extends ColumnConverter[BigDecimal] {
  def sqlType(column: ColumnLike[BigDecimal]) = "DECIMAL(20, 2)"
  def toSQLType(column: ColumnLike[BigDecimal], value: BigDecimal) = value.underlying()
  def fromSQLType(column: ColumnLike[BigDecimal], value: Any) = BigDecimal(value.asInstanceOf[java.math.BigDecimal])
}

object StringConverter extends ColumnConverter[String] {
  val VarcharType = s"VARCHAR(${Int.MaxValue})"
  val VarcharIngoreCaseType = s"VARCHAR_IGNORECASE(${Int.MaxValue})"

  def sqlType(column: ColumnLike[String]) = column match {
    case c: Column[_] if c.has(IgnoreCase) => VarcharIngoreCaseType
    case _ => VarcharType
  }
  def toSQLType(column: ColumnLike[String], value: String) = value
  def fromSQLType(column: ColumnLike[String], value: Any) = value.asInstanceOf[String]
}

object WrappedStringConverter extends ColumnConverter[WrappedString] {
  def sqlType(column: ColumnLike[WrappedString]) = column match {
    case c: Column[_] if c.has(IgnoreCase) => StringConverter.VarcharIngoreCaseType
    case _ => StringConverter.VarcharType
  }
  def toSQLType(column: ColumnLike[WrappedString], value: WrappedString) = value.value
  def fromSQLType(column: ColumnLike[WrappedString], value: Any) = column.manifest.runtimeClass.create(Map("value" -> value.asInstanceOf[String]))
}

object ByteArrayConverter extends ColumnConverter[Array[Byte]] {
  def sqlType(column: ColumnLike[Array[Byte]]) = "BINARY(1000)"
  def toSQLType(column: ColumnLike[Array[Byte]], value: Array[Byte]) = value
  def fromSQLType(column: ColumnLike[Array[Byte]], value: Any) = value.asInstanceOf[Array[Byte]]
}

object BlobConverter extends ColumnConverter[Blob] {
  def sqlType(column: ColumnLike[Blob]) = "BLOB"
  def toSQLType(column: ColumnLike[Blob], value: Blob) = value
  def fromSQLType(column: ColumnLike[Blob], value: Any) = value.asInstanceOf[Blob]
}

object TimestampConverter extends ColumnConverter[Timestamp] {
  def sqlType(column: ColumnLike[Timestamp]) = "TIMESTAMP"
  def toSQLType(column: ColumnLike[Timestamp], value: Timestamp) = value
  def fromSQLType(column: ColumnLike[Timestamp], value: Any) = value.asInstanceOf[Timestamp]
}

class EnumConverter[T <: EnumEntry](implicit manifest: Manifest[T]) extends ColumnConverter[T] {
  val enumerated = manifest.runtimeClass.instance.getOrElse(throw new RuntimeException("Unable to find companion for ${manifest.runtimeClass}")).asInstanceOf[Enumerated[T]]

  def sqlType(column: ColumnLike[T]) = s"VARCHAR(${Int.MaxValue})"
  def toSQLType(column: ColumnLike[T], value: T) = value match {
    case null => null
    case _ => value.name
  }
  def fromSQLType(column: ColumnLike[T], value: Any) = enumerated(value.asInstanceOf[String])
}