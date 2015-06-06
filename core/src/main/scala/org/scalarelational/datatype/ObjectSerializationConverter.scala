package org.scalarelational.datatype

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.scalarelational.ColumnLike

/**
 * ObjectSerializationConverter stores any arbitrary serializable object as a Array[Byte].
 *
 * @author Matt Hicks <matt@outr.com>
 */
class ObjectSerializationConverter[T <: AnyRef] extends DataType[T] {
  def sqlType(column: ColumnLike[T]) = s"BINARY(${Int.MaxValue})"

  def toSQLType(column: ColumnLike[T], value: T) = if (value != null) {
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

  def fromSQLType(column: ColumnLike[T], value: Any) = value match {
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
