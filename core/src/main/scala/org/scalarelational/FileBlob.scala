package org.scalarelational

import java.io._
import java.sql.Blob


class FileBlob(file: File) extends Blob {
  def length() = file.length()

  def getBytes(pos: Long, length: Int) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def getBinaryStream = new BufferedInputStream(new FileInputStream(file))

  def position(pattern: Array[Byte], start: Long) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def position(pattern: Blob, start: Long) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def setBytes(pos: Long, bytes: Array[Byte]) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def setBytes(pos: Long, bytes: Array[Byte], offset: Int, len: Int) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def setBinaryStream(pos: Long) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def truncate(len: Long) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")

  def free() = {}

  def getBinaryStream(pos: Long, length: Long) = throw new UnsupportedOperationException("FileBlob doesn't yet support this operation.")
}
