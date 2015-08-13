package org.scalarelational.datatype.create

import org.scalarelational.datatype.{DataType, OptionSQLConversion, OptionSQLOperator}

/**
 * @author Matt Hicks <matt@outr.com>
 */
object OptionDataTypeCreator {
  def create[T, S](dt: DataType[T, S]) = {
    val conversion = new OptionSQLConversion(dt.converter)
    val operator = new OptionSQLOperator[T, S]
    new DataType[Option[T], S](dt.jdbcType, dt.sqlType, conversion, operator)
  }
}