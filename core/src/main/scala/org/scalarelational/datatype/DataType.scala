package org.scalarelational.datatype

import java.sql.{Blob, Timestamp}

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.powerscala.reflect._
import org.scalarelational.column.WrappedString
import org.scalarelational.column.property.{ColumnProperty, ColumnLength, IgnoreCase, NumericStorage}
import org.scalarelational.model.{ColumnPropertyContainer, ColumnLike}
import org.scalarelational.op.Operator

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DataType[T] {
  def sqlType(properties: ColumnPropertyContainer): String
  def sqlOperator(column: ColumnLike[_], value: T, op: Operator): Operator = op
  def toSQLType(column: ColumnLike[_], value: T): Any
  def fromSQLType(column: ColumnLike[_], value: Any): T
}

object DataTypeGenerators {
  def option[T](implicit dt: DataType[T]): DataType[Option[T]] = new DataType[Option[T]] {
    def sqlType(properties: ColumnPropertyContainer) = dt.sqlType(properties)

    override def sqlOperator(column: ColumnLike[_], value: Option[T], op: Operator): Operator =
      (value, op) match {
        case (None, Operator.Equal) => Operator.Is
        case (None, Operator.NotEqual) => Operator.IsNot
        case (None, _) => throw new RuntimeException(s"Operator $op cannot take None (column = $column)")
        case (Some(t), _) => op
      }

    def toSQLType(column: ColumnLike[_], value: Option[T]): Any =
      value match {
        case None => null
        case Some(t) => dt.toSQLType(column, t)
      }

    def fromSQLType(column: ColumnLike[_], value: Any): Option[T] =
      value match {
        case null => None
        case t => Some(dt.fromSQLType(column, t))
      }
  }
}

object BooleanDataType extends DataType[Boolean] {
  def sqlType(properties: ColumnPropertyContainer) = "BOOLEAN"
  def toSQLType(column: ColumnLike[_], value: Boolean) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Boolean]
}

object IntDataType extends DataType[Int] {
  def sqlType(properties: ColumnPropertyContainer) = "INTEGER"
  def toSQLType(column: ColumnLike[_], value: Int) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Int]
}

object JavaIntDataType extends DataType[java.lang.Integer] {
  def sqlType(properties: ColumnPropertyContainer) = "INTEGER"
  def toSQLType(column: ColumnLike[_], value: java.lang.Integer) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[java.lang.Integer]
}

object LongDataType extends DataType[Long] {
  def sqlType(properties: ColumnPropertyContainer) = "BIGINT"
  def toSQLType(column: ColumnLike[_], value: Long) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Long]
}

object JavaLongDataType extends DataType[java.lang.Long] {
  def sqlType(properties: ColumnPropertyContainer) = "BIGINT"
  def toSQLType(column: ColumnLike[_], value: java.lang.Long) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[java.lang.Long]
}

object DoubleDataType extends DataType[Double] {
  def sqlType(properties: ColumnPropertyContainer) = "DOUBLE"
  def toSQLType(column: ColumnLike[_], value: Double) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Double]
}

object JavaDoubleDataType extends DataType[java.lang.Double] {
  def sqlType(properties: ColumnPropertyContainer) = "DOUBLE"
  def toSQLType(column: ColumnLike[_], value: java.lang.Double) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[java.lang.Double]
}

object BigDecimalDataType extends DataType[BigDecimal] {
  def sqlType(properties: ColumnPropertyContainer) = {
    val numericStorage = properties.get[NumericStorage](NumericStorage.Name).getOrElse(NumericStorage.DefaultBigDecimal)
    s"DECIMAL(${numericStorage.precision}, ${numericStorage.scale})"
  }
  def toSQLType(column: ColumnLike[_], value: BigDecimal) = value.underlying()
  def fromSQLType(column: ColumnLike[_], value: Any) = BigDecimal(value.asInstanceOf[java.math.BigDecimal])
}

object StringDataType extends DataType[String] {
  def sqlType(properties: ColumnPropertyContainer) = {
    val length = properties.get[ColumnLength](ColumnLength.Name).map(_.length)
        .getOrElse(ColumnLength.DefaultVarChar)
    if (properties.has(IgnoreCase)) s"VARCHAR_IGNORECASE($length)"
    else s"VARCHAR($length)"
  }
  def toSQLType(column: ColumnLike[_], value: String) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[String]
}

object WrappedStringDataType extends DataType[WrappedString] {
  def sqlType(properties: ColumnPropertyContainer) = StringDataType.sqlType(properties)
  def toSQLType(column: ColumnLike[_], value: WrappedString) = value.value
  def fromSQLType(column: ColumnLike[_], value: Any) = column.manifest.runtimeClass.create(Map("value" -> value.asInstanceOf[String]))
}

object ByteArrayDataType extends DataType[Array[Byte]] {
  def sqlType(properties: ColumnPropertyContainer) = {
    val length = properties.get[ColumnLength](ColumnLength.Name)
        .map(_.length)
        .getOrElse(ColumnLength.DefaultBinary)
    s"BINARY($length)"
  }
  def toSQLType(column: ColumnLike[_], value: Array[Byte]) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Array[Byte]]
}

object BlobDataType extends DataType[Blob] {
  def sqlType(properties: ColumnPropertyContainer) = "BLOB"
  def toSQLType(column: ColumnLike[_], value: Blob) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Blob]
}

object TimestampDataType extends DataType[Timestamp] {
  def sqlType(properties: ColumnPropertyContainer) = "TIMESTAMP"
  def toSQLType(column: ColumnLike[_], value: Timestamp) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Timestamp]
}

class EnumDataType[T <: EnumEntry](implicit manifest: Manifest[T]) extends DataType[T] {
  val enumerated = manifest.runtimeClass.instance
    .getOrElse(throw new RuntimeException(s"Unable to find companion for ${manifest.runtimeClass}"))
    .asInstanceOf[Enumerated[T]]

  val length = enumerated.values.maxBy(_.name.length).name.length

  def sqlType(properties: ColumnPropertyContainer) = s"VARCHAR($length)"
  def toSQLType(column: ColumnLike[_], value: T) = value.name
  def fromSQLType(column: ColumnLike[_], value: Any) =
    enumerated(value.asInstanceOf[String])
}

trait DataTypes {
  implicit def booleanConverter = BooleanDataType
  implicit def intConverter = IntDataType
  implicit def longConverter = LongDataType
  implicit def doubleConverter = DoubleDataType
  implicit def bigDecimalConverter = BigDecimalDataType
  implicit def stringConverter = StringDataType
  implicit def wrappedStringConverter = WrappedStringDataType
  implicit def byteArrayConverter = ByteArrayDataType
  implicit def blobConverter = BlobDataType
  implicit def timestampConverter = TimestampDataType
  implicit def javaIntConverter = JavaIntDataType
  implicit def javaLongConverter = JavaLongDataType
  implicit def javaDoubleConverter = JavaDoubleDataType
  implicit def option[T: DataType] = DataTypeGenerators.option[T]
}
