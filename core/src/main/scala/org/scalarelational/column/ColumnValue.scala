package org.scalarelational.column

import org.scalarelational.ExpressionValue
import org.scalarelational.datatype.DataType


class ColumnValue[T, S] private(val column: ColumnLike[T, S],
                                val value: T,
                                val converterOverride: Option[DataType[T, S]]) extends ExpressionValue[T] {
  def expression: ColumnLike[T, S] = column
  def toSQL: Any =
    try {
      converterOverride.getOrElse(column.dataType).converter.toSQL(value)
    } catch {
      case t: Throwable =>
        val targetClass = if (value != null) value.getClass else null
        throw new RuntimeException(s"Invalid conversion to $targetClass (table = ${column.table}, column = $column, value = $value)", t)
    }

  override def toString = s"$column: $value"

  override def equals(obj: scala.Any) = obj match {
    case cv: ColumnValue[_, _] => cv.column == column && cv.value == value
    case _ => false
  }
}

object ColumnValue {
  def apply[T, S](column: ColumnLike[T, S], value: T, converterOverride: Option[DataType[T, S]]): ColumnValue[T, S] =
    new ColumnValue[T, S](column, value, converterOverride)
}
