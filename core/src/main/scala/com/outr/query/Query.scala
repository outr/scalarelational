package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Query(columns: List[Column[_]], table: Table = null) {
  def from(table: Table) = copy(table = table)
}