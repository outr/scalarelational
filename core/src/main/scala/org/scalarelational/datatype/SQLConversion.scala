package org.scalarelational.datatype

import org.scalarelational.column.ColumnLike

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait SQLConversion[T, S] {
  def toSQL(column: ColumnLike[T, S], value: T): S
  def fromSQL(column: ColumnLike[T, S], value: S): T
}

object SQLConversion {
  def identity[T] = new SQLConversion[T, T] {
    override def toSQL(column: ColumnLike[T, T], value: T): T = value

    override def fromSQL(column: ColumnLike[T, T], value: T): T = value
  }
}

class OptionSQLConversion[T, S](underlying: SQLConversion[T, S]) extends SQLConversion[Option[T], S] {
  override def toSQL(column: ColumnLike[Option[T], S], value: Option[T]): S = value match {
    case None => null.asInstanceOf[S]
    case Some(t) => underlying.toSQL(column.asInstanceOf[ColumnLike[T, S]], t)
  }

  override def fromSQL(column: ColumnLike[Option[T], S], value: S): Option[T] = value match {
    case null => None
    case t => Some(underlying.fromSQL(column.asInstanceOf[ColumnLike[T, S]], t))
  }
}

class RefSQLConversion[T] extends SQLConversion[Ref[T], Int] {
  override def toSQL(column: ColumnLike[Ref[T], Int], value: Ref[T]) = value.id
  override def fromSQL(column: ColumnLike[Ref[T], Int], value: Int) = new Ref[T](value)
}