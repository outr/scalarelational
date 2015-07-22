package org.scalarelational.instruction

import org.scalarelational.column.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait Insert {
  def rows: Seq[Seq[ColumnValue[_]]]

  /**
   * Returns a new copy of this Insert with an additional column value added to
   * each row. Will replace if the column is already represented.
   */
  def add(value: ColumnValue[_]): Insert
}
