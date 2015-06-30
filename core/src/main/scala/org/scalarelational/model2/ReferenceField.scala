package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
class ReferenceField[T](field: Field[T], tableAlias: Option[String] = None) extends Field[T] {
  override def tableName = tableAlias.orElse(field.tableName)
  override def name = field.name

  override def toSQL = {
    val fieldSQL = field.toSQL
    val ref = tableAlias match {
      case Some(t) => s"$t.${field.name}"
      case None => fieldSQL.text
    }
    SQL(ref, fieldSQL.args)
  }
}