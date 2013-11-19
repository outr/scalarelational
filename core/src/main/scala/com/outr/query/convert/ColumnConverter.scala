package com.outr.query.convert

import com.outr.query.ColumnLike
import org.powerscala.enum.{Enumerated, EnumEntry}
import org.powerscala.reflect._

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ColumnConverter[T] {
  def sqlType: String

  def toSQLType(column: ColumnLike[T], value: T): Any

  def fromSQLType(column: ColumnLike[T], value: Any): T
}

object BooleanConverter extends ColumnConverter[Boolean] {
  def sqlType = "BOOLEAN"
  def toSQLType(column: ColumnLike[Boolean], value: Boolean) = value
  def fromSQLType(column: ColumnLike[Boolean], value: Any) = value.asInstanceOf[Boolean]
}

object IntConverter extends ColumnConverter[Int] {
  def sqlType = "INTEGER"
  def toSQLType(column: ColumnLike[Int], value: Int) = value
  def fromSQLType(column: ColumnLike[Int], value: Any) = value.asInstanceOf[Int]
}

object LongConverter extends ColumnConverter[Long] {
  def sqlType = "BIGINT"
  def toSQLType(column: ColumnLike[Long], value: Long) = value
  def fromSQLType(column: ColumnLike[Long], value: Any) = value.asInstanceOf[Long]
}

object DoubleConverter extends ColumnConverter[Double] {
  def sqlType = "DOUBLE"
  def toSQLType(column: ColumnLike[Double], value: Double) = value
  def fromSQLType(column: ColumnLike[Double], value: Any) = value.asInstanceOf[Double]
}

object BigDecimalConverter extends ColumnConverter[BigDecimal] {
  def sqlType = "DECIMAL(20, 2)"
  def toSQLType(column: ColumnLike[BigDecimal], value: BigDecimal) = value
  def fromSQLType(column: ColumnLike[BigDecimal], value: Any) = value.asInstanceOf[BigDecimal]
}

object StringConverter extends ColumnConverter[String] {
  val sqlType = s"VARCHAR(${Int.MaxValue})"
  def toSQLType(column: ColumnLike[String], value: String) = value
  def fromSQLType(column: ColumnLike[String], value: Any) = value.asInstanceOf[String]
}

object ByteArrayConverter extends ColumnConverter[Array[Byte]] {
  def sqlType = "BINARY(1000)"
  def toSQLType(column: ColumnLike[Array[Byte]], value: Array[Byte]) = value
  def fromSQLType(column: ColumnLike[Array[Byte]], value: Any) = value.asInstanceOf[Array[Byte]]
}

class EnumConverter[T <: EnumEntry](implicit manifest: Manifest[T]) extends ColumnConverter[T] {
  val enumerated = manifest.runtimeClass.instance.getOrElse(throw new RuntimeException("Unable to find companion for ${manifest.runtimeClass}")).asInstanceOf[Enumerated[T]]

  def sqlType = s"VARCHAR(${Int.MaxValue})"
  def toSQLType(column: ColumnLike[T], value: T) = value.name
  def fromSQLType(column: ColumnLike[T], value: Any) = enumerated(value.asInstanceOf[String])
}