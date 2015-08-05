package org.scalarelational.datatype

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class DataTyped[T](dataType: DataType[T], value: T)