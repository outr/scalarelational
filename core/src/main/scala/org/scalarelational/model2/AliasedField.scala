package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
class AliasedField[T](val field: Field[T], val alias: String) extends Field[T] {
  override def tableName = field.tableName

  override def name = alias

  override def toSQL = {
    val fieldSQL = field.toSQL
    SQL(s"${fieldSQL.text} AS [$alias]", fieldSQL.args)
  }
}