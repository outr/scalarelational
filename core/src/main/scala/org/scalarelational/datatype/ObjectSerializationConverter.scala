package org.scalarelational.datatype

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.Types

import org.scalarelational.column.ColumnLike

/**
 * ObjectSerializationConverter stores any arbitrary serializable object as a Array[Byte].
 *
 * @author Matt Hicks <matt@outr.com>
 */
class ObjectSerializationDataTypeCreator[T <: AnyRef](implicit manifest: Manifest[T]) extends DataTypeCreator[T] {
  def create() = DataType[T](Types.BINARY, SQLType(s"BINARY(${Int.MaxValue})"), new ObjectSQLConverter[T])
}

class ObjectSQLConverter[T] extends SQLConversion[T, Array[Byte]] {
  override def toSQL(column: ColumnLike[_], value: T): Array[Byte] = if (value != null) {
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

  override def fromSQL(column: ColumnLike[_], value: Array[Byte]): T = value match {
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
