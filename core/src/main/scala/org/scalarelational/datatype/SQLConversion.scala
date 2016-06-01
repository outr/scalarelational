package org.scalarelational.datatype

trait SQLConversion[T, S] {
  def toSQL(value: T): S
  def fromSQL(value: S): T
}

object SQLConversion {
  def identity[T]: SQLConversion[T, T] = new SQLConversion[T, T] {
    override def toSQL(value: T): T = value
    override def fromSQL(value: T): T = value
  }
}

class OptionSQLConversion[T, S](underlying: SQLConversion[T, S]) extends SQLConversion[Option[T], S] {
  override def toSQL(value: Option[T]): S = value match {
    case None => None.orNull.asInstanceOf[S]
    case Some(t) => underlying.toSQL(t)
  }

  override def fromSQL(value: S): Option[T] = Option(value) match {
    case None => None
    case Some(t) => Some(underlying.fromSQL(t))
  }
}

class RefSQLConversion[T] extends SQLConversion[Ref[T], Int] {
  override def toSQL(value: Ref[T]): Int = value.id
  override def fromSQL(value: Int): Ref[T] = new Ref[T](value)
}