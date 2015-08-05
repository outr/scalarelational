package org.scalarelational.datatype

import java.sql.{JDBCType, SQLType, Blob, Timestamp}

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.powerscala.reflect._
import org.scalarelational.WrappedString
import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.column.property.{IgnoreCase, ColumnLength, NumericStorage}
import org.scalarelational.model.Datastore
import org.scalarelational.op.Operator

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DataType[T] {
  def jdbcType: SQLType
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer): String
  def sqlOperator(column: ColumnLike[_], value: T, op: Operator): Operator = op
  def toSQLType(column: ColumnLike[_], value: T): Any
  def fromSQLType(column: ColumnLike[_], value: Any): T
  def typed(value: T) = DataTyped[T](this, value)
}

trait MappedDataType[T, S] extends DataType[T] {
  override def toSQLType(column: ColumnLike[_], value: T): S
  def fromSQLTyped(column: ColumnLike[_], value: S): T

  override final def fromSQLType(column: ColumnLike[_], value: Any): T = fromSQLTyped(column, value.asInstanceOf[S])
}

object DataTypeGenerators {
  def option[T](implicit dt: DataType[T]): DataType[Option[T]] = new DataType[Option[T]] {
    override def jdbcType = dt.jdbcType

    def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = dt.sqlType(datastore, properties)

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


    override def equals(obj: scala.Any) = toString == obj.toString

    override def toString = s"OptionDataType($dt)"
  }

  def ref[T]: DataType[Ref[T]] = new DataType[Ref[T]] {
    override def jdbcType = JDBCType.INTEGER
    def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "INTEGER"
    def toSQLType(column: ColumnLike[_], value: Ref[T]) = value.id
    def fromSQLType(column: ColumnLike[_], value: Any) = new Ref[T](value.asInstanceOf[Int])
    override def toString = s"RefDataType"
  }
}

object BooleanDataType extends DataType[Boolean] {
  override def jdbcType = JDBCType.BOOLEAN
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "BOOLEAN"
  def toSQLType(column: ColumnLike[_], value: Boolean) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Boolean]
}

object IntDataType extends DataType[Int] {
  override def jdbcType = JDBCType.INTEGER
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "INTEGER"
  def toSQLType(column: ColumnLike[_], value: Int) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Int]
}

object JavaIntDataType extends DataType[java.lang.Integer] {
  override def jdbcType = JDBCType.INTEGER
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "INTEGER"
  def toSQLType(column: ColumnLike[_], value: java.lang.Integer) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[java.lang.Integer]
}

object LongDataType extends DataType[Long] {
  override def jdbcType = JDBCType.BIGINT
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "BIGINT"
  def toSQLType(column: ColumnLike[_], value: Long) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Long]
}

object JavaLongDataType extends DataType[java.lang.Long] {
  override def jdbcType = JDBCType.BIGINT
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "BIGINT"
  def toSQLType(column: ColumnLike[_], value: java.lang.Long) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[java.lang.Long]
}

object DoubleDataType extends DataType[Double] {
  override def jdbcType = JDBCType.DOUBLE
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "DOUBLE"
  def toSQLType(column: ColumnLike[_], value: Double) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Double]
}

object JavaDoubleDataType extends DataType[java.lang.Double] {
  override def jdbcType = JDBCType.DOUBLE
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "DOUBLE"
  def toSQLType(column: ColumnLike[_], value: java.lang.Double) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[java.lang.Double]
}

object BigDecimalDataType extends DataType[BigDecimal] {
  override def jdbcType = JDBCType.DECIMAL
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = {
    val numericStorage = properties.get[NumericStorage](NumericStorage.Name).getOrElse(NumericStorage.DefaultBigDecimal)
    s"DECIMAL(${numericStorage.precision}, ${numericStorage.scale})"
  }
  def toSQLType(column: ColumnLike[_], value: BigDecimal) = value.underlying()
  def fromSQLType(column: ColumnLike[_], value: Any) = BigDecimal(value.asInstanceOf[java.math.BigDecimal])
}

object StringDataType extends DataType[String] {
  override def jdbcType = JDBCType.VARCHAR
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = {
    val length = properties.get[ColumnLength](ColumnLength.Name).map(_.length)
        .getOrElse(datastore.DefaultVarCharLength)
    if (properties.has(IgnoreCase)) s"VARCHAR_IGNORECASE($length)"
    else s"VARCHAR($length)"
  }
  def toSQLType(column: ColumnLike[_], value: String) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[String]
}

object WrappedStringDataType extends DataType[WrappedString] {
  override def jdbcType = StringDataType.jdbcType
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = StringDataType.sqlType(datastore, properties)
  def toSQLType(column: ColumnLike[_], value: WrappedString) = value.value
  def fromSQLType(column: ColumnLike[_], value: Any) = column.manifest.runtimeClass.create(Map("value" -> value.asInstanceOf[String]))
}

object ByteArrayDataType extends DataType[Array[Byte]] {
  override def jdbcType = JDBCType.BINARY
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = {
    val length = properties.get[ColumnLength](ColumnLength.Name)
        .map(_.length)
        .getOrElse(datastore.DefaultBinaryLength)
    s"BINARY($length)"
  }
  def toSQLType(column: ColumnLike[_], value: Array[Byte]) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Array[Byte]]
}

object BlobDataType extends DataType[Blob] {
  override def jdbcType = JDBCType.BLOB
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "BLOB"
  def toSQLType(column: ColumnLike[_], value: Blob) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Blob]
}

object TimestampDataType extends DataType[Timestamp] {
  override def jdbcType = JDBCType.TIMESTAMP
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "TIMESTAMP"
  def toSQLType(column: ColumnLike[_], value: Timestamp) = value
  def fromSQLType(column: ColumnLike[_], value: Any) = value.asInstanceOf[Timestamp]
}

object LongTimestampDataType extends MappedDataType[Long, Timestamp] {
  override def jdbcType = JDBCType.TIMESTAMP
  override def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "TIMESTAMP"
  override def toSQLType(column: ColumnLike[_], value: Long) = new Timestamp(value)
  override def fromSQLTyped(column: ColumnLike[_], value: Timestamp) = value.getTime
}

class EnumDataType[T <: EnumEntry](implicit manifest: Manifest[T]) extends DataType[T] {
  val enumerated = manifest.runtimeClass.instance
    .getOrElse(throw new RuntimeException(s"Unable to find companion for ${manifest.runtimeClass}"))
    .asInstanceOf[Enumerated[T]]

  val length = enumerated.values.maxBy(_.name.length).name.length

  override def jdbcType = JDBCType.VARCHAR
  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = s"VARCHAR($length)"
  def toSQLType(column: ColumnLike[_], value: T) = value.name
  def fromSQLType(column: ColumnLike[_], value: Any) =
    enumerated(value.asInstanceOf[String])
}

trait DataTypes {
  implicit def booleanDataType = BooleanDataType
  implicit def intDataType = IntDataType
  implicit def longDataType = LongDataType
  implicit def doubleDataType = DoubleDataType
  implicit def bigDecimalDataType = BigDecimalDataType
  implicit def stringDataType = StringDataType
  implicit def wrappedStringDataType = WrappedStringDataType
  implicit def byteArrayDataType = ByteArrayDataType
  implicit def blobDataType = BlobDataType
  implicit def timestampDataType = TimestampDataType
  implicit def javaIntDataType = JavaIntDataType
  implicit def javaLongDataType = JavaLongDataType
  implicit def javaDoubleDataType = JavaDoubleDataType
  implicit def option[T: DataType] = DataTypeGenerators.option[T]
  implicit def reference[T] = DataTypeGenerators.ref[T]
  implicit def longTimestampDataType = LongTimestampDataType
}
