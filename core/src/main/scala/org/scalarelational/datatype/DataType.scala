package org.scalarelational.datatype

import java.sql.{Blob, Timestamp, Types}

import org.powerscala.enum.{EnumEntry, Enumerated}
import org.powerscala.reflect._
import org.scalarelational.WrappedString
import org.scalarelational.column.property.{ColumnLength, IgnoreCase, NumericStorage}
import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.model.Datastore
import org.scalarelational.op.Operator

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class DataType[T, S](jdbcType: Int,
                       sqlType: SQLType,
                       converter: SQLConversion[T, S],
                       sqlOperator: SQLOperator[T] = new DefaultSQLOperator[T])(implicit manifest: Manifest[T]) {
  def scalaClass = manifest.runtimeClass
  def typed(value: T) = TypedValue(this, value)
}

trait SQLType {
  def apply(datastore: Datastore, properties: ColumnPropertyContainer): String
}

object SQLType {
  def apply(value: String) = new SimpleSQLType(value)
  def f(f: (Datastore, ColumnPropertyContainer) => String) = new SQLType {
    override def apply(datastore: Datastore, properties: ColumnPropertyContainer): String = f(datastore, properties)
  }
}

class SimpleSQLType(value: String) extends SQLType {
  def apply(datastore: Datastore, properties: ColumnPropertyContainer) = value
}

object StringSQLType extends SQLType {
  override def apply(datastore: Datastore, properties: ColumnPropertyContainer) = {
    val length = properties.get[ColumnLength](ColumnLength.Name).map(_.length)
      .getOrElse(datastore.DefaultVarCharLength)
    if (properties.has(IgnoreCase)) s"VARCHAR_IGNORECASE($length)"
    else s"VARCHAR($length)"
  }
}

trait SQLOperator[T] {
  def apply(column: ColumnLike[T, _], value: T, op: Operator): Operator
}

class DefaultSQLOperator[T] extends SQLOperator[T] {
  def apply(column: ColumnLike[T, _], value: T, op: Operator): Operator = op
}

class OptionSQLOperator[T] extends SQLOperator[Option[T]] {
  override def apply(column: ColumnLike[Option[T], _], value: Option[T], op: Operator): Operator = (value, op) match {
    case (None, Operator.Equal) => Operator.Is
    case (None, Operator.NotEqual) => Operator.IsNot
    case (None, _) => throw new RuntimeException(s"Operator $op cannot take None (column = $column)")
    case (Some(t), _) => op
  }
}

trait SQLConversion[T, S] {
  def toSQL(column: ColumnLike[T, S], value: T): S
  def fromSQL(column: ColumnLike[T, S], value: S): T
}

object SQLConversion {
  def identity[T] = new SQLConversion[T, T] {
    override def toSQL(column: ColumnLike[T, T], value: T): T = value

    override def fromSQL(column: ColumnLike[T, T], value: T): T = value
  }
  def f[T, S](to: (ColumnLike[T, S], T) => S)(from: (ColumnLike[T, S], S) => T) = {
    new SQLConversion[T, S] {
      override def toSQL(column: ColumnLike[T, S], value: T): S = to(column, value)

      override def fromSQL(column: ColumnLike[T, S], value: S): T = from(column, value)
    }
  }
}

class OptionSQLConversion[T, S](underlying: SQLConversion[T, S]) extends SQLConversion[Option[T], S] {
  override def toSQL(column: ColumnLike[Option[T], S], value: Option[T]): S = value match {
    case None => null.asInstanceOf[S]
    case Some(t) => underlying.toSQL(column.asInstanceOf[ColumnLike[T, S]], t)
  }

  override def fromSQL(column: ColumnLike[Option[T], S], value: S): Option[T] = value match {
    case null => None
    case t => Some(underlying.fromSQL(column.asInstanceOf[ColumnLike[T, S]], t))
  }
}

class RefSQLConversion[T] extends SQLConversion[Ref[T], Int] {
  override def toSQL(column: ColumnLike[Ref[T], Int], value: Ref[T]) = value.id
  override def fromSQL(column: ColumnLike[Ref[T], Int], value: Int) = new Ref[T](value)
}

trait DataTypeCreator[ScalaType, SQLType] {
  def create(): DataType[ScalaType, SQLType]
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
  val TimestampCreator = apply(DataTypes.Timestamp)
  val WrappedStringCreator = apply(DataTypes.WrapString)
  val LongTimestampCreator = apply(DataTypes.LongTimestamp)

  def apply[ScalaType, SQLType](dataType: DataType[ScalaType, SQLType]) = new DataTypeCreator[ScalaType, SQLType] {
    override def create(): DataType[ScalaType, SQLType] = dataType
  }
}

object DataTypes {
  val BigDec = DataType[BigDecimal, java.math.BigDecimal](Types.DECIMAL, SQLType.f {
    case (datastore, properties) => {
      val numericStorage = properties.get[NumericStorage](NumericStorage.Name).getOrElse(NumericStorage.DefaultBigDecimal)
      s"DECIMAL(${numericStorage.precision}, ${numericStorage.scale})"
    }
  }, SQLConversion.f[BigDecimal, java.math.BigDecimal] {
    case (column: ColumnLike[BigDecimal, java.math.BigDecimal], value: BigDecimal) => value.underlying()
  } {
    case (column: ColumnLike[BigDecimal, java.math.BigDecimal], value: java.math.BigDecimal) => BigDecimal(value)
  })
  val Boolean = DataType[Boolean, Boolean](Types.BOOLEAN, SQLType("BOOLEAN"), SQLConversion.identity)
  val Blob = DataType[Blob, Blob](Types.BLOB, SQLType("BLOB"), SQLConversion.identity)
  val ByteArray = DataType[Array[Byte], Array[Byte]](Types.BINARY, SQLType.f {
    case (datastore, properties) => {
      val length = properties.get[ColumnLength](ColumnLength.Name)
        .map(_.length)
        .getOrElse(datastore.DefaultBinaryLength)
      s"BINARY($length)"
    }
  }, SQLConversion.identity)
  val Double = DataType[Double, Double](Types.DOUBLE, SQLType("DOUBLE"), SQLConversion.identity)
  val Int = DataType[Int, Int](Types.INTEGER, SQLType("INTEGER"), SQLConversion.identity)
  val Long = DataType[Long, Long](Types.BIGINT, SQLType("BIGINT"), SQLConversion.identity)
  val String = DataType[String, String](Types.VARCHAR, StringSQLType, SQLConversion.identity)
  val Timestamp = DataType[Timestamp, Timestamp](Types.TIMESTAMP, SQLType("TIMESTAMP"), SQLConversion.identity)
  val WrapString = DataType[WrappedString, String](String.jdbcType, String.sqlType, SQLConversion.f[WrappedString, String] {
    case (column: ColumnLike[WrappedString, String], value: WrappedString) => value.value
  } {
    case (column: ColumnLike[WrappedString, String], value: String) => column.manifest.runtimeClass.create[WrappedString](Map("value" -> value))
  })

  val LongTimestamp = DataType[Long, Timestamp](Types.TIMESTAMP, SQLType("TIMESTAMP"), SQLConversion.f[Long, Timestamp] {
    case (column: ColumnLike[Long, Timestamp], value: Long) => new Timestamp(value)
  } {
    case (column: ColumnLike[Long, Timestamp], value: Timestamp) => value.getTime
  })
}

// Special Generic types

class EnumDataTypeCreator[T <: EnumEntry](implicit manifest: Manifest[T]) extends DataTypeCreator[T, String] with SQLConversion[T, String] {
  val enumerated = manifest.runtimeClass.instance
    .getOrElse(throw new RuntimeException(s"Unable to find companion for ${manifest.runtimeClass}"))
    .asInstanceOf[Enumerated[T]]

  val length = math.max(enumerated.values.maxBy(_.name.length).name.length, 128)

  override def create() = DataType[T, String](Types.VARCHAR, SQLType(s"VARCHAR($length)"), this)

  override def toSQL(column: ColumnLike[T, String], value: T): String = value.name
  override def fromSQL(column: ColumnLike[T, String], value: String): T = enumerated(value)
}
class OptionDataTypeCreator[T, S](dt: DataType[T, S])(implicit manifest: Manifest[Option[T]]) extends DataTypeCreator[Option[T], S] {
  def this()(implicit dtc: DataTypeCreator[T, S], manifest: Manifest[Option[T]]) = this(dtc.create())

  override def create() = DataType[Option[T], S](dt.jdbcType, dt.sqlType, new OptionSQLConversion(dt.converter), new OptionSQLOperator[T])
}
class RefDataTypeCreator[T](implicit manifest: Manifest[Ref[T]]) extends DataTypeCreator[Ref[T], Int] {
  override def create() = DataType[Ref[T], Int](Types.INTEGER, SQLType("INTEGER"), new RefSQLConversion[T])
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
  implicit def timestampTypeCreator = TimestampCreator
  implicit def wrappedStringTypeCreator = WrappedStringCreator

  implicit def longTimestampTypeCreator = LongTimestampCreator

  implicit def option[T, S](implicit creator: DataTypeCreator[T, S], manifest: Manifest[Option[T]]) = new OptionDataTypeCreator[T, S]
  implicit def reference[T](implicit manifest: Manifest[Ref[T]]) = new RefDataTypeCreator[T]

  implicit def enum[T <: EnumEntry](implicit manifest: Manifest[T]) = new EnumDataTypeCreator[T]()
}