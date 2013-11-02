package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class ColumnValue[T](column: Column[T], value: T)