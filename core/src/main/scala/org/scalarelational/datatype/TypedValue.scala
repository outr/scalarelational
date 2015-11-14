package org.scalarelational.datatype


class TypedValue[T, S](val dataType: DataType[T, S], val value: T) {
  override def equals(obj: scala.Any) = obj match {
    case tv: TypedValue[_, _] => tv.value == value
    case _ => false
  }
}