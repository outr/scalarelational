package org.scalarelational.column.property

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */
case class ColumnLength(length: Int) extends ColumnProperty {
  def name = ColumnLength.Name
}

object ColumnLength {
  val Name = "columnLength"
}
