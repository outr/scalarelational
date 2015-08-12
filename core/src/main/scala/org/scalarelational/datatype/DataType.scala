package org.scalarelational.datatype

import java.sql.{Blob, Timestamp, Types}

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.powerscala.reflect._
import org.scalarelational.WrappedString
import org.scalarelational.column.property.{ColumnLength, IgnoreCase, NumericStorage}
import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.model.Datastore
import org.scalarelational.op.Operator

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class DataType[T](jdbcType: Int,
                       dbType: DBType,
                       converter: SQLConversion[T, _] = new IdenticalSQLConversion[T],
                       sqlOperator: SQLOperator[T] = new DefaultSQLOperator[T])(implicit manifest: Manifest[T]) {
  def scalaClass = manifest.runtimeClass
  def typed(value: T) = TypedValue[T](this, value)
}

trait DBType {
  def apply(datastore: Datastore, properties: ColumnPropertyContainer): String
}

object DBType {
  def apply(value: String) = new SimpleDBType(value)
  def f(f: (Datastore, ColumnPropertyContainer) => String) = new DBType {
    override def apply(datastore: Datastore, properties: ColumnPropertyContainer): String = f(datastore, properties)
  }
}

class SimpleDBType(value: String) extends DBType {
  def apply(datastore: Datastore, properties: ColumnPropertyContainer) = value
}

object StringDBType extends DBType {
  override def apply(datastore: Datastore, properties: ColumnPropertyContainer) = {
    val length = properties.get[ColumnLength](ColumnLength.Name).map(_.length)
      .getOrElse(datastore.DefaultVarCharLength)
    if (properties.has(IgnoreCase)) s"VARCHAR_IGNORECASE($length)"
    else s"VARCHAR($length)"
  }
}

trait SQLOperator[T] {
  def apply(column: ColumnLike[T], value: T, op: Operator): Operator
}

class DefaultSQLOperator[T] extends SQLOperator[T] {
  def apply(column: ColumnLike[T], value: T, op: Operator): Operator = op
}

class OptionSQLOperator[T] extends SQLOperator[Option[T]] {
  override def apply(column: ColumnLike[Option[T]], value: Option[T], op: Operator): Operator = (value, op) match {
    case (None, Operator.Equal) => Operator.Is
    case (None, Operator.NotEqual) => Operator.IsNot
    case (None, _) => throw new RuntimeException(s"Operator $op cannot take None (column = $column)")
    case (Some(t), _) => op
  }
}

trait SQLConversion[ScalaType, SQLType] {
  def toSQL(column: ColumnLike[_], value: ScalaType): SQLType
  def fromSQL(column: ColumnLike[_], value: SQLType): ScalaType
}

object SQLConversion {
  def apply[T] = new IdenticalSQLConversion[T]
  def f[ScalaType, SQLType](to: (ColumnLike[_], ScalaType) => SQLType)(from: (ColumnLike[_], SQLType) => ScalaType) = {
    new SQLConversion[ScalaType, SQLType] {
      override def toSQL(column: ColumnLike[_], value: ScalaType): SQLType = to(column, value)

      override def fromSQL(column: ColumnLike[_], value: SQLType): ScalaType = from(column, value)
    }
  }
}

class IdenticalSQLConversion[T] extends SQLConversion[T, T] {
  override def toSQL(column: ColumnLike[_], value: T): T = value

  override def fromSQL(column: ColumnLike[_], value: T): T = value
}

class OptionSQLConversion[ScalaType, SQLType](underlying: SQLConversion[ScalaType, SQLType]) extends SQLConversion[Option[ScalaType], SQLType] {
  override def toSQL(column: ColumnLike[_], value: Option[ScalaType]): SQLType = value match {
    case None => null.asInstanceOf[SQLType]
    case Some(t) => underlying.toSQL(column, t)
  }

  override def fromSQL(column: ColumnLike[_], value: SQLType): Option[ScalaType] = value match {
    case null => None
    case t => Some(underlying.fromSQL(column, t))
  }
}

class RefSQLConversion[T] extends SQLConversion[Ref[T], Int] {
  override def toSQL(column: ColumnLike[_], value: Ref[T]) = value.id
  override def fromSQL(column: ColumnLike[_], value: Int) = new Ref[T](value)
}

trait DataTypeCreator[T] {
  def create(): DataType[T]
}

object DataTypeCreator {
  val BigDecimalCreator = apply(DataTypes.BigDec)
  val BooleanCreator = apply(DataTypes.Boolean)
  val BlobCreator = apply(DataTypes.Blob)
  val ByteArrayCreator = apply(DataTypes.ByteArray)
  val DoubleCreator = apply(DataTypes.Double)
  val IntCreator = apply(DataTypes.Int)
  val LongCreator = apply(DataTypes.Long)
  val StringCreator = apply(DataTypes.String)

  def apply[T](dataType: DataType[T]) = new DataTypeCreator[T] {
    override def create(): DataType[T] = dataType
  }
}

trait MappedDataTypeCreator[ScalaType, SQLType] {
  def create(): DataType[ScalaType]
}

object DataTypes {
  val BigDec = DataType[BigDecimal](Types.DECIMAL, DBType.f {
    case (datastore, properties) => {
      val numericStorage = properties.get[NumericStorage](NumericStorage.Name).getOrElse(NumericStorage.DefaultBigDecimal)
      s"DECIMAL(${numericStorage.precision}, ${numericStorage.scale})"
    }
  }, SQLConversion.f[BigDecimal, java.math.BigDecimal] {
    case (column: ColumnLike[_], value: BigDecimal) => value.underlying()
  } {
    case (column: ColumnLike[_], value: java.math.BigDecimal) => BigDecimal(value)
  })
  val Boolean = DataType[Boolean](Types.BOOLEAN, DBType("BOOLEAN"))
  val Blob = DataType[Blob](Types.BLOB, DBType("BLOB"))
  val ByteArray = DataType[Array[Byte]](Types.BINARY, DBType.f {
    case (datastore, properties) => {
      val length = properties.get[ColumnLength](ColumnLength.Name)
        .map(_.length)
        .getOrElse(datastore.DefaultBinaryLength)
      s"BINARY($length)"
    }
  })
  val Double = DataType[Double](Types.DOUBLE, DBType("DOUBLE"))
  val Int = DataType[Int](Types.INTEGER, DBType("INTEGER"))
  val Long = DataType[Long](Types.BIGINT, DBType("BIGINT"))
  val String = DataType[String](Types.VARCHAR, StringDBType)
  val Timestamp = DataType[Timestamp](Types.TIMESTAMP, DBType("TIMESTAMP"))
  val WrapString = DataType[WrappedString](String.jdbcType, String.dbType, SQLConversion.f[WrappedString, String] {
    case (column: ColumnLike[_], value: WrappedString) => value.value
  } {
    case (column: ColumnLike[_], value: String) => column.manifest.runtimeClass.create[WrappedString](Map("value" -> value))
  })

  val LongTimestamp = DataType[Long](Types.TIMESTAMP, DBType("TIMESTAMP"), SQLConversion.f[Long, Timestamp] {
    case (column: ColumnLike[_], value: Long) => new Timestamp(value)
  } {
    case (column: ColumnLike[_], value: Timestamp) => value.getTime
  })
}

object TimestampDataTypeCreator extends DataTypeCreator[Timestamp] {
  override def create() = DataTypes.Timestamp
}
object WrappedStringDataTypeCreator extends DataTypeCreator[WrappedString] {
  override def create() = DataTypes.WrapString
}

// Mapped types

object LongTimestampDataTypeCreator extends MappedDataTypeCreator[Long, Timestamp] {
  override def create() = DataTypes.LongTimestamp
}

// Special Generic types

class EnumDataTypeCreator[T <: EnumEntry](implicit manifest: Manifest[T]) extends DataTypeCreator[T] with SQLConversion[T, String] {
  val enumerated = manifest.runtimeClass.instance
    .getOrElse(throw new RuntimeException(s"Unable to find companion for ${manifest.runtimeClass}"))
    .asInstanceOf[Enumerated[T]]

  val length = math.max(enumerated.values.maxBy(_.name.length).name.length, 128)

  override def create() = DataType[T](Types.VARCHAR, DBType(s"VARCHAR($length)"), this)

  override def toSQL(column: ColumnLike[_], value: T): String = value.name
  override def fromSQL(column: ColumnLike[_], value: String): T = enumerated(value)
}
class OptionDataTypeCreator[T](dt: DataType[T])(implicit manifest: Manifest[Option[T]]) extends DataTypeCreator[Option[T]] {
  def this()(implicit dtc: DataTypeCreator[T], manifest: Manifest[Option[T]]) = this(dtc.create())

  override def create() = DataType[Option[T]](dt.jdbcType, dt.dbType, new OptionSQLConversion(dt.converter), new OptionSQLOperator[T])
}
class RefDataTypeCreator[T](implicit manifest: Manifest[Ref[T]]) extends DataTypeCreator[Ref[T]] {
  override def create() = DataType[Ref[T]](Types.INTEGER, DBType("INTEGER"), new RefSQLConversion[T])
}

trait DataTypeSupport {
  // TODO: eventually convert this to create the DataType instead
  import DataTypeCreator._
  implicit def bigDecimalTypeCreator = BigDecimalCreator
  implicit def booleanTypeCreator = BooleanCreator
  implicit def blobTypeCreator = BlobCreator
  implicit def byteArrayTypeCreator = ByteArrayCreator
  implicit def doubleTypeCreator = DoubleCreator
  implicit def intTypeCreator = IntCreator
  implicit def longTypeCreator = LongCreator
  implicit def stringTypeCreator = StringCreator
  implicit def timestampTypeCreator = TimestampDataTypeCreator
  implicit def wrappedStringTypeCreator = WrappedStringDataTypeCreator

  implicit def longTimestampTypeCreator = LongTimestampDataTypeCreator

  implicit def option[T: DataTypeCreator](implicit manifest: Manifest[Option[T]]): DataTypeCreator[Option[T]] = new OptionDataTypeCreator[T]
  implicit def reference[T](implicit manifest: Manifest[Ref[T]]): DataTypeCreator[Ref[T]] = new RefDataTypeCreator[T]

  implicit def enum[T <: EnumEntry](implicit manifest: Manifest[T]): DataTypeCreator[T] = new EnumDataTypeCreator[T]()
}