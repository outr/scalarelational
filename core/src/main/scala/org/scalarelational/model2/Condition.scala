package org.scalarelational.model2

import scala.util.matching.Regex

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Condition extends ModelEntry {
  def and(condition: Condition) = Conditions(List(this, condition), ConnectType.And)
  def or(condition: Condition) = Conditions(List(this, condition), ConnectType.Or)
}

case class NullCondition[T](field: Field[T], operator: Operator) extends Condition {
  override def toSQL = {
    val fieldSQL = field.toSQL
    SQL(s"${fieldSQL.text} ${operator.symbol} NULL", fieldSQL.args)
  }
}

case class ColumnCondition[T](field: Field[T], operator: Operator, other: Field[T]) extends Condition {
  override def toSQL = {
    val fieldSQL = field.toSQL
    val otherSQL = other.toSQL
    SQL(s"${fieldSQL.text} ${operator.symbol} ${otherSQL.text}", fieldSQL.args ::: otherSQL.args)
  }
}

case class DirectCondition[T](field: Field[T], operator: Operator, value: T) extends Condition {
  override def toSQL = {
    val fieldSQL = field.toSQL
    SQL(s"${fieldSQL.text} ${operator.symbol} ?", fieldSQL.args ::: List(value))
  }
}

case class RangeCondition[T](field: Field[T], operator: Operator, values: Seq[T]) extends Condition {
  override def toSQL = throw new RuntimeException("NOT IMPLEMENTED")
}

case class LikeCondition[T](field: Field[T], pattern: String, not: Boolean) extends Condition {
  override def toSQL = throw new RuntimeException("NOT IMPLEMENTED")
}

case class RegexCondition[T](field: Field[T], regex: Regex, not: Boolean) extends Condition {
  override def toSQL = throw new RuntimeException("NOT IMPLEMENTED")
}

case class Conditions(list: List[Condition], connectType: ConnectType = ConnectType.And) extends Condition {
  override def toSQL = throw new RuntimeException("NOT IMPLEMENTED")

  override def and(condition: Condition) = if (connectType == ConnectType.And) {
    copy(list = (condition :: list.reverse).reverse)
  } else {
    throw new RuntimeException("Cannot add AND for conditions already connected with OR.")
  }

  override def or(condition: Condition) = if (connectType == ConnectType.Or) {
    copy(list = (condition :: list.reverse).reverse)
  } else {
    throw new RuntimeException("Cannot add OR for conditions already connected with AND.")
  }
}
