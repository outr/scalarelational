package org.scalarelational

import org.powerscala.reflect._
import org.scalarelational.datatype.DataType
import org.scalarelational.model.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ColumnValue[T] private(val column: ColumnLike[T],
                             val value: T,
                             val converterOverride: Option[DataType[T]]) extends ExpressionValue[T] {
  def expression = column
  def toSQL = converterOverride match {
    case Some(converter) => converter.toSQLType(column, value)
    case None => column.converter.toSQLType(column, value)
  }

  override def toString = s"$column = $value"
}

object ColumnValue {
  def apply[T](column: ColumnLike[T], value: T, converterOverride: Option[DataType[T]]) = {
    val v = if (value.asInstanceOf[AnyRef] != null && !value.getClass.hasType(column.manifest.runtimeClass)) {
      EnhancedMethod.convertTo(column.name, value, column.manifest.runtimeClass).asInstanceOf[T]
    } else {
      value
    }
    new ColumnValue[T](column, v, converterOverride)
  }
}