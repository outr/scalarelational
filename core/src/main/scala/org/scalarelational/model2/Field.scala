package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Field[T] extends ModelEntry {
  def tableName: Option[String]
  def name: String

  def as(alias: String) = new AliasedField(this, alias)

  def ===(field: Field[T]) = ColumnCondition(this, Operator.Equal, field)
}