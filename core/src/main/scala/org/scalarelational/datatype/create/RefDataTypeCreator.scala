package org.scalarelational.datatype.create

import java.sql.Types

import org.scalarelational.datatype.{DataType, Ref, RefSQLConversion, SQLType}


object RefDataTypeCreator {
  def create[T]: DataType[Ref[T], Int] = {
    new DataType[Ref[T], Int](Types.INTEGER, SQLType("INTEGER"), new RefSQLConversion[T])
  }
}