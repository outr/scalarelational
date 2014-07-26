package com.outr.query.orm.convert

import com.outr.query.{QueryResult, Column}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Option2ValueConverter[T] extends ORMConverter[T, Option[T]] {
  def fromORM(column: Column[T], o: Option[T]) = o match {
    case Some(i) => Conversion(Some(column(i)), None)
    case None => Conversion.empty
  }

  def toORM(column: Column[T], c: T, result: QueryResult) = Some(Option(c))
}

object Option2ValueConverter {
  def apply[SQLType, ValueType](toValue: SQLType => Option[ValueType], toSQL: Option[ValueType] => SQLType) = {
    new ConvertingOption2ValueConverter[SQLType, ValueType](toValue, toSQL)
  }
}

class ConvertingOption2ValueConverter[SQLType, ValueType](toValue: SQLType => Option[ValueType], toSQL: Option[ValueType] => SQLType) extends ORMConverter[SQLType, Option[ValueType]] {
  override def fromORM(column: Column[SQLType], o: Option[ValueType]): Conversion[SQLType, Option[ValueType]] = {
    Conversion(Some(column(toSQL(o))), None)
  }

  override def toORM(column: Column[SQLType], c: SQLType, result: QueryResult): Option[Option[ValueType]] = {
    Some(toValue(c))
  }
}