package org.scalarelational.datatype.create

import java.sql.Types

import org.scalarelational.datatype.{DataType, Ref, RefSQLConversion, SQLType}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object RefDataTypeCreator {
  def create[T] = new DataType[Ref[T], Int](Types.INTEGER, SQLType("INTEGER"), new RefSQLConversion[T])
}