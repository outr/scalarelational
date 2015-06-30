package org.scalarelational.model2

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait ModelEntry {
  def toSQL: SQL
}