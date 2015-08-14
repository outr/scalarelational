package org.scalarelational.datatype

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class DataType[T, S](val jdbcType: Int,
                     val sqlType: SQLType,
                     val converter: SQLConversion[T, S],
                     val sqlOperator: SQLOperator[T, S] = new DefaultSQLOperator[T, S]) {
  def typed(value: T) = TypedValue(this, value)

  def copy(jdbcType: Int = jdbcType,
           sqlType: SQLType = sqlType,
           converter: SQLConversion[T, S] = converter,
           sqlOperator: SQLOperator[T, S] = sqlOperator) = {
    new DataType[T, S](jdbcType, sqlType, converter, sqlOperator)
  }
}