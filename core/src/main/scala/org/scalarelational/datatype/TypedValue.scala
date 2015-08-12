package org.scalarelational.datatype

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class TypedValue[T, S](dataType: DataType[T, S], value: T) {
  override def equals(obj: scala.Any) = obj match {
    case tv: TypedValue[_, _] => tv.value == value
    case _ => false
  }
}