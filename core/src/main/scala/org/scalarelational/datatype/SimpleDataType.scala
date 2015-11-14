package org.scalarelational.datatype


class SimpleDataType[T](jdbcType: Int,
                        sqlType: SQLType,
                        converter: SQLConversion[T, T] = SQLConversion.identity[T],
                        sqlOperator: SQLOperator[T, T] = new DefaultSQLOperator[T, T])
  extends DataType[T, T](jdbcType, sqlType, converter, sqlOperator)