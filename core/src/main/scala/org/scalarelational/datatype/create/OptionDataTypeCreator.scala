package org.scalarelational.datatype.create

import org.scalarelational.datatype.{DataType, OptionSQLConversion, OptionSQLOperator}

object OptionDataTypeCreator {
  def create[T, S](dt: DataType[T, S]): DataType[Option[T], S] = {
    val conversion = new OptionSQLConversion(dt.converter)
    val operator = new OptionSQLOperator[T, S]
    new DataType[Option[T], S](dt.jdbcType, dt.sqlType, conversion, operator, optional = true)
  }
}