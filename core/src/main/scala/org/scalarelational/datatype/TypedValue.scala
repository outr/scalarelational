package org.scalarelational.datatype

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TypedValue[T](dataType: DataType[T], value: T) {
  override def equals(obj: scala.Any) = obj match {
    case tv: TypedValue[_] => tv.value == value
    case _ => false
  }
}