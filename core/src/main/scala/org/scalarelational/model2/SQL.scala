package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class SQL(text: String, args: List[Any] = Nil)

object SQL {
  def merge(entries: SQL*) = {
    val s = entries.map(sql => sql.text).mkString
    val args = entries.flatMap(sql => sql.args).toList
    SQL(s, args)
  }
}