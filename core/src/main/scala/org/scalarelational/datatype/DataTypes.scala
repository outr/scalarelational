package org.scalarelational.datatype

import java.math
import java.sql.{Blob, Timestamp, Types}

import org.powerscala.reflect._
import org.scalarelational.WrappedString
import org.scalarelational.column.property.{ColumnLength, NumericStorage}
import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.model.Datastore

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

  val BigDecimalType = simplify(new DataType[BigDecimal, java.math.BigDecimal](Types.DECIMAL, new SQLType {
    override def apply(datastore: Datastore, properties: ColumnPropertyContainer) = {
      val numericStorage = properties.get[NumericStorage](NumericStorage.Name).getOrElse(NumericStorage.DefaultBigDecimal)
      s"DECIMAL(${numericStorage.precision}, ${numericStorage.scale})"
    }
  }, new SQLConversion[BigDecimal, java.math.BigDecimal] {

    override def toSQL(column: ColumnLike[BigDecimal, math.BigDecimal], value: BigDecimal) = value.underlying()

    override def fromSQL(column: ColumnLike[BigDecimal, math.BigDecimal], value: math.BigDecimal) = BigDecimal(value)
  }))
  val BooleanType = new SimpleDataType[Boolean](Types.BOOLEAN, SQLType("BOOLEAN"))
  val BlobType = new SimpleDataType[Blob](Types.BLOB, SQLType("BLOB"))
  val ByteArrayType = new SimpleDataType[Array[Byte]](Types.BINARY, new SQLType {
    override def apply(datastore: Datastore, properties: ColumnPropertyContainer) = {
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
  val WrappedStringType = simplify(new DataType[WrappedString, String](StringType.jdbcType, StringType.sqlType, new SQLConversion[WrappedString, String] {
    override def toSQL(column: ColumnLike[WrappedString, String], value: WrappedString) = value.value

    override def fromSQL(column: ColumnLike[WrappedString, String], value: String) = column.manifest.runtimeClass.create[WrappedString](Map("value" -> value))
  }))

  val LongTimestampType = new DataType[Long, Timestamp](Types.TIMESTAMP, SQLType("TIMESTAMP"), new SQLConversion[Long, Timestamp] {
    override def toSQL(column: ColumnLike[Long, Timestamp], value: Long) = new Timestamp(value)

    override def fromSQL(column: ColumnLike[Long, Timestamp], value: Timestamp) = value.getTime
  })
}