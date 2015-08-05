package org.scalarelational.datatype

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.Types

import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.model.Datastore

/**
 * ObjectSerializationConverter stores any arbitrary serializable object as a Array[Byte].
 *
 * @author Matt Hicks <matt@outr.com>
 */
class ObjectSerializationConverter[T <: AnyRef] extends DataType[T] {
  override def jdbcType = Types.BINARY

  def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = s"BINARY(${Int.MaxValue})"

  def toSQLType(column: ColumnLike[_], value: T) = if (value != null) {
    val baos = new ByteArrayOutputStream()
    try {
      val oos = new ObjectOutputStream(baos)
      try {
        oos.writeObject(value)
        oos.flush()
        baos.toByteArray
      } finally {
        oos.close()
      }
    } finally {
      baos.close()
    }
  } else {
    null
  }

  def fromSQLType(column: ColumnLike[_], value: Any) = value match {
    case null => null.asInstanceOf[T]
    case array: Array[Byte] => {
      val bais = new ByteArrayInputStream(array)
      try {
        val ois = new ObjectInputStream(bais)
        try {
          ois.readObject().asInstanceOf[T]
        } finally {
          ois.close()
        }
      } finally {
        bais.close()
      }
    }
  }
}
