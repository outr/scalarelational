package org.scalarelational.datatype

import java.math
import java.sql.{Blob, Timestamp, Types}

import org.powerscala.reflect._
import org.scalarelational.WrappedString
import org.scalarelational.column.property.{ColumnLength, NumericStorage}
import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.model.Datastore


object DataTypes {
  def simplify[T, S](dataType: DataType[T, S]): SimpleDataType[T] = new SimpleDataType[T](
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
  object BooleanType extends SimpleDataType[Boolean](Types.BOOLEAN, SQLType("BOOLEAN"))
  object BlobType extends SimpleDataType[Blob](Types.BLOB, new BlobSQLType("BLOB"))
  object ByteArrayType extends SimpleDataType[Array[Byte]](Types.BINARY, new SQLType {
    override def apply(datastore: Datastore, properties: ColumnPropertyContainer): String = {
      val length = properties.get[ColumnLength](ColumnLength.Name)
        .map(_.length)
        .getOrElse(datastore.DefaultBinaryLength)
      s"BINARY($length)"
    }
  }, SQLConversion.identity)
  object DoubleType extends SimpleDataType[Double](Types.DOUBLE, SQLType("DOUBLE"))
  object IntType extends SimpleDataType[Int](Types.INTEGER, SQLType("INTEGER"))
  object LongType extends SimpleDataType[Long](Types.BIGINT, SQLType("BIGINT"))
  object StringType extends SimpleDataType[String](Types.VARCHAR, StringSQLType)
  object TimestampType extends SimpleDataType[Timestamp](Types.TIMESTAMP, SQLType("TIMESTAMP"))
  val WrappedStringType = simplify(new DataType[WrappedString, String](StringType.jdbcType, StringType.sqlType, new SQLConversion[WrappedString, String] {
    override def toSQL(column: ColumnLike[WrappedString, String], value: WrappedString) = value.value

    override def fromSQL(column: ColumnLike[WrappedString, String], value: String) = column.manifest.runtimeClass.create[WrappedString](Map("value" -> value))
  }))

  object LongTimestampType extends DataType[Long, Timestamp](Types.TIMESTAMP, SQLType("TIMESTAMP"), new SQLConversion[Long, Timestamp] {
    override def toSQL(column: ColumnLike[Long, Timestamp], value: Long): Timestamp = new Timestamp(value)

    override def fromSQL(column: ColumnLike[Long, Timestamp], value: Timestamp): Long = value.getTime
  })
}