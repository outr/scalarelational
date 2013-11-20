package com.outr.query.orm.convert

import com.outr.query.ColumnValue

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed trait Conversion[C, O]

object Conversion {
  def empty[C, O] = EmptyConversion.asInstanceOf[Conversion[C, O]]
  def apply[C, O](columnValue: Option[ColumnValue[C]], updated: Option[O] = None) = {
    ConversionResponse(columnValue, updated)
  }
}

case class ConversionResponse[C, O](columnValue: Option[ColumnValue[C]], updated: Option[O]) extends Conversion[C, O]

object EmptyConversion extends Conversion[Nothing, Nothing]