package com.outr

/**
 * @author Matt Hicks <matt@outr.com>
 */
package object query {
  def select(columns: Column[_]*) = Query(columns.toList)
}