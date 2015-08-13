package org.scalarelational.datatype

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.Types

import org.scalarelational.column.ColumnLike

/**
 * ObjectSerializationConverter stores any arbitrary serializable object as a Array[Byte].
 *
 * @author Matt Hicks <matt@outr.com>
 */
object ObjectSerializationDataTypeCreator {
  def create[T <: AnyRef](implicit manifest: Manifest[T]) = {
    new DataType[T, Array[Byte]](Types.BINARY, SQLType(s"BINARY(${Int.MaxValue})"), new ObjectSQLConverter[T])
  }
}

class ObjectSQLConverter[T] extends SQLConversion[T, Array[Byte]] {
  override def toSQL(column: ColumnLike[T, Array[Byte]], value: T): Array[Byte] = if (value != null) {
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

  override def fromSQL(column: ColumnLike[T, Array[Byte]], value: Array[Byte]): T = value match {
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
