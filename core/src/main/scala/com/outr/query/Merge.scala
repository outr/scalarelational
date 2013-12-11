package com.outr.query

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Merge(key: Column[_], values: List[ColumnValue[_]])