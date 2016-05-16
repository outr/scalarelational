//package org.scalarelational.h2.column.types
//
//import org.scalarelational.column.types.ColumnType
//
//case class DecimalType(columnName: Option[String] = None,
//                       defaultValue: Option[BigDecimal] = None) extends ColumnType[BigDecimal] {
//  def name(name: String): DecimalType = copy(columnName = Option(name))
//  def default(value: BigDecimal): DecimalType = copy(defaultValue = Some(value))
//}
//
//object DecimalType extends DecimalType(None, None)