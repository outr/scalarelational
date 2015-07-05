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
      converterOverride match {
        case Some(converter) => converter.toSQLType(column, value)
        case None => column.converter.toSQLType(column, value)
      }
    } catch {
      case t: Throwable =>
        val sourceClass = column.manifest.runtimeClass
        val targetClass = value.getClass
        throw new RuntimeException(s"Invalid conversion from $sourceClass to $targetClass (column = $column, value = $value)")
    }

  override def toString = s"$column: $value"
}

object ColumnValue {
  def apply[T](column: ColumnLike[T], value: T, converterOverride: Option[DataType[T]]): ColumnValue[T] =
    new ColumnValue[T](column, value, converterOverride)
}