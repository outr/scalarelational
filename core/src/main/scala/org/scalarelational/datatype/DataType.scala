package org.scalarelational.datatype


class DataType[T, S](val jdbcType: Int,
                     val sqlType: SQLType,
                     val converter: SQLConversion[T, S],
                     val sqlOperator: SQLOperator[T, S] = new DefaultSQLOperator[T, S]) {
  def typed(value: T): TypedValue[T, S] = new TypedValue(this, value)

  def copy(jdbcType: Int = jdbcType,
           sqlType: SQLType = sqlType,
           converter: SQLConversion[T, S] = converter,
           sqlOperator: SQLOperator[T, S] = sqlOperator): DataType[T, S] = {
    new DataType[T, S](jdbcType, sqlType, converter, sqlOperator)
  }
}