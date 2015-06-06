package org.scalarelational

import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class Merge(key: Column[_], values: List[ColumnValue[_]])