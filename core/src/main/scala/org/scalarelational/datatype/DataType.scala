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
class DataType[T, S](val jdbcType: Int,
                     val sqlType: SQLType,
                     val converter: SQLConversion[T, S],
                     val sqlOperator: SQLOperator[T, S] = new DefaultSQLOperator[T, S])
                    (implicit val manifest: Manifest[T]) {
  def scalaClass = manifest.runtimeClass
  def typed(value: T) = TypedValue(this, value)

  def copy(jdbcType: Int = jdbcType,
           sqlType: SQLType = sqlType,
           converter: SQLConversion[T, S] = converter,
           sqlOperator: SQLOperator[T, S] = sqlOperator) = {
    new DataType[T, S](jdbcType, sqlType, converter, sqlOperator)
  }
}

class SimpleDataType[T](jdbcType: Int,
                        sqlType: SQLType,
                        converter: SQLConversion[T, T] = SQLConversion.identity[T],
                        sqlOperator: SQLOperator[T, T] = new DefaultSQLOperator[T, T])(implicit manifest: Manifest[T])
      extends DataType[T, T](jdbcType, sqlType, converter, sqlOperator)

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

trait SQLOperator[T, S] {
  def apply(column: ColumnLike[T, S], value: T, op: Operator): Operator
}

class DefaultSQLOperator[T, S] extends SQLOperator[T, S] {
  def apply(column: ColumnLike[T, S], value: T, op: Operator): Operator = op
}

class OptionSQLOperator[T, S] extends SQLOperator[Option[T], S] {
  override def apply(column: ColumnLike[Option[T], S], value: Option[T], op: Operator): Operator = (value, op) match {
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

object DataTypes {
  def simplify[T, S](dataType: DataType[T, S]) = new SimpleDataType[T](
    jdbcType = dataType.jdbcType,
    sqlType = dataType.sqlType,
    converter = dataType.converter.asInstanceOf[SQLConversion[T, T]],
    sqlOperator = dataType.sqlOperator.asInstanceOf[SQLOperator[T, T]]
  )(dataType.manifest)

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

// Special Generic types

class EnumDataTypeCreator[T <: EnumEntry](implicit manifest: Manifest[T]) extends SQLConversion[T, String] {
  val enumerated = manifest.runtimeClass.instance
    .getOrElse(throw new RuntimeException(s"Unable to find companion for ${manifest.runtimeClass}"))
    .asInstanceOf[Enumerated[T]]

  val length = math.max(enumerated.values.maxBy(_.name.length).name.length, 128)

  def create() = new DataType[T, String](Types.VARCHAR, SQLType(s"VARCHAR($length)"), this)

  override def toSQL(column: ColumnLike[T, String], value: T): String = value.name
  override def fromSQL(column: ColumnLike[T, String], value: String): T = enumerated(value)
}
object OptionDataTypeCreator {
  def create[T, S](dt: DataType[T, S])(implicit manifest: Manifest[Option[T]]) = {
    val conversion = new OptionSQLConversion(dt.converter)
    val operator = new OptionSQLOperator[T, S]
    new DataType[Option[T], S](dt.jdbcType, dt.sqlType, conversion, operator)
  }
}
object RefDataTypeCreator {
  def create[T](implicit manifest: Manifest[Ref[T]]) = {
    new DataType[Ref[T], Int](Types.INTEGER, SQLType("INTEGER"), new RefSQLConversion[T])
  }
}

trait DataTypeSupport {
  import DataTypes._

  implicit def bigDecimalType = BigDecimal
  implicit def booleanType = BooleanType
  implicit def blobType = BlobType
  implicit def byteArrayType = ByteArrayType
  implicit def doubleType = DoubleType
  implicit def intType = IntType
  implicit def longType = LongType
  implicit def stringType = StringType
  implicit def timestampType = TimestampType
  implicit def wrappedStringType = WrappedStringType

  implicit def longTimestampType = LongTimestampType

  implicit def option[T, S](implicit dataType: DataType[T, S], manifest: Manifest[Option[T]]) = {
    OptionDataTypeCreator.create[T, S](dataType)
  }
  implicit def reference[T](implicit manifest: Manifest[Ref[T]]) = RefDataTypeCreator.create[T](manifest)

  implicit def enum[T <: EnumEntry](implicit manifest: Manifest[T]) = new EnumDataTypeCreator[T].create()
}