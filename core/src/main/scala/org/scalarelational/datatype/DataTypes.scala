package org.scalarelational.datatype

import java.sql.{Blob, Timestamp, Types}

import org.powerscala.reflect._
import org.scalarelational.WrappedString
import org.scalarelational.column.ColumnLike
import org.scalarelational.column.property.{ColumnLength, NumericStorage}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object DataTypes {
  def simplify[T, S](dataType: DataType[T, S]) = new SimpleDataType[T](
    jdbcType = dataType.jdbcType,
    sqlType = dataType.sqlType,
    converter = dataType.converter.asInstanceOf[SQLConversion[T, T]],
    sqlOperator = dataType.sqlOperator.asInstanceOf[SQLOperator[T, T]]
  )

  val BigDecimalType = simplify(new DataType[BigDecimal, java.math.BigDecimal](Types.DECIMAL, SQLType.f {
    case (datastore, properties) => {
      val numericStorage = properties.get[NumericStorage](NumericStorage.Name).getOrElse(NumericStorage.DefaultBigDecimal)
      s"DECIMAL(${numericStorage.precision}, ${numericStorage.scale})"
    }
  }, SQLConversion.f[BigDecimal, java.math.BigDecimal] {
    case (column: ColumnLike[BigDecimal, java.math.BigDecimal], value: BigDecimal) => value.underlying()
  } {
    case (column: ColumnLike[BigDecimal, java.math.BigDecimal], value: java.math.BigDecimal) => BigDecimal(value)
  }))
  val BooleanType = new SimpleDataType[Boolean](Types.BOOLEAN, SQLType("BOOLEAN"))
  val BlobType = new SimpleDataType[Blob](Types.BLOB, SQLType("BLOB"))
  val ByteArrayType = new SimpleDataType[Array[Byte]](Types.BINARY, SQLType.f {
    case (datastore, properties) => {
      val length = properties.get[ColumnLength](ColumnLength.Name)
        .map(_.length)
        .getOrElse(datastore.DefaultBinaryLength)
      s"BINARY($length)"
    }
  }, SQLConversion.identity)
  val DoubleType = new SimpleDataType[Double](Types.DOUBLE, SQLType("DOUBLE"))
  val IntType = new SimpleDataType[Int](Types.INTEGER, SQLType("INTEGER"))
  val LongType = new SimpleDataType[Long](Types.BIGINT, SQLType("BIGINT"))
  val StringType = new SimpleDataType[String](Types.VARCHAR, StringSQLType)
  val TimestampType = new SimpleDataType[Timestamp](Types.TIMESTAMP, SQLType("TIMESTAMP"))
  val WrappedStringType = simplify(new DataType[WrappedString, String](StringType.jdbcType, StringType.sqlType, SQLConversion.f[WrappedString, String] {
    case (column: ColumnLike[WrappedString, String], value: WrappedString) => value.value
  } {
    case (column: ColumnLike[WrappedString, String], value: String) => column.manifest.runtimeClass.create[WrappedString](Map("value" -> value))
  }))

  val LongTimestampType = new DataType[Long, Timestamp](Types.TIMESTAMP, SQLType("TIMESTAMP"), SQLConversion.f[Long, Timestamp] {
    case (column: ColumnLike[Long, Timestamp], value: Long) => new Timestamp(value)
  } {
    case (column: ColumnLike[Long, Timestamp], value: Timestamp) => value.getTime
  })
}