package org.scalarelational.datatype

import java.io.{ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.sql.{Blob, Types}
import javax.sql.rowset.serial.SerialBlob

import org.scalarelational.column.ColumnLike

/**
 * ObjectSerializationConverter stores any arbitrary serializable object as a Blob.
 *
 * @author Matt Hicks <matt@outr.com>
 */
object ObjectSerializationDataTypeCreator {
  def create[T <: AnyRef] =
    new DataType[T, Blob](Types.BLOB, new BlobSQLType("BLOB"), new ObjectSQLConverter[T])
}

class ObjectSQLConverter[T] extends SQLConversion[T, Blob] {
  override def toSQL(value: T): Blob = {
    val baos = new ByteArrayOutputStream()
    try {
      val oos = new ObjectOutputStream(baos)
      try {
        oos.writeObject(value)
        oos.flush()
        new SerialBlob(baos.toByteArray)
      } finally {
        oos.close()
      }
    } finally {
      baos.close()
    }
  }

  override def fromSQL(value: Blob): T = {
    val ois = new ObjectInputStream(value.getBinaryStream)
    try {
      ois.readObject().asInstanceOf[T]
    } finally {
      ois.close()
    }
  }
}
