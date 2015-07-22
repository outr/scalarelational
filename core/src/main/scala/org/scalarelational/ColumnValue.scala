package org.scalarelational

import org.scalarelational.datatype.DataType
import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ColumnValue[T] private(val column: ColumnLike[T],
                             val value: T,
                             val converterOverride: Option[DataType[T]]) extends ExpressionValue[T] {
  def expression: ColumnLike[T] = column
  def toSQL: Any =
    try {
      converterOverride.getOrElse(column.dataType).toSQLType(column, value)
    } catch {
      case t: Throwable =>
        val sourceClass = column.manifest.runtimeClass
        val targetClass = value.getClass
        throw new RuntimeException(s"Invalid conversion from $sourceClass to $targetClass (table = ${column.table}, column = $column, value = $value)", t)
    }

  override def toString = s"$column: $value"

  override def equals(obj: scala.Any) = obj match {
    case cv: ColumnValue[_] => cv.column == column && cv.value == value
    case _ => false
  }
}

object ColumnValue {
  def apply[T](column: ColumnLike[T], value: T, converterOverride: Option[DataType[T]]): ColumnValue[T] =
    new ColumnValue[T](column, value, converterOverride)
}
