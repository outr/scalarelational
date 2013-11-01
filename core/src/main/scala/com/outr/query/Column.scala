package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Column[T](name: String,
                     default: Option[T] = None,
                     notNull: Boolean = false,
                     autoIncrement: Boolean = false,
                     primaryKey: Boolean = false,
                     unique: Boolean = false)
                    (implicit val manifest: Manifest[T], table: Table) {
}
