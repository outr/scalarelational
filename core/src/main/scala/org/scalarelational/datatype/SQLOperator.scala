package org.scalarelational.datatype

import org.scalarelational.column.ColumnLike
import org.scalarelational.op.Operator

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLOperator[T, S] {
  def apply(column: ColumnLike[T, S], value: T, op: Operator): Operator
}

class DefaultSQLOperator[T, S] extends SQLOperator[T, S] {
  def apply(column: ColumnLike[T, S], value: T, op: Operator): Operator = op
}

class OptionSQLOperator[T, S] extends SQLOperator[Option[T], S] {
  override def apply(column: ColumnLike[Option[T], S], value: Option[T], op: Operator): Operator = (value, op) match {
    case (None, Operator.Equal) => Operator.Is
    case (None, Operator.NotEqual) => Operator.IsNot
    case (None, _) => throw new RuntimeException(s"Operator $op cannot take None (column = $column)")
    case (Some(t), _) => op
  }
}