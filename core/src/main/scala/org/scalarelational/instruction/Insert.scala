package org.scalarelational.instruction

import org.scalarelational.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Insert {
  def rows: Seq[Seq[ColumnValue[_]]]
}
