package org.scalarelational

import java.io._
import java.sql.Blob


class FileBlob(file: File) extends Blob {
  def length(): Long = file.length()

  def getBytes(pos: Long, length: Int): Array[Byte] = {
    throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")
  }

  def getBinaryStream: InputStream = new BufferedInputStream(new FileInputStream(file))

  def position(pattern: Array[Byte], start: Long): Long = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def position(pattern: Blob, start: Long): Long = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def setBytes(pos: Long, bytes: Array[Byte]): Int = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def setBytes(pos: Long, bytes: Array[Byte], offset: Int, len: Int): Int = {
    throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")
  }

  def setBinaryStream(pos: Long): OutputStream = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def truncate(len: Long): Unit = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def free(): Unit = {}

  def getBinaryStream(pos: Long, length: Long): InputStream = {
    throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")
  }
}
