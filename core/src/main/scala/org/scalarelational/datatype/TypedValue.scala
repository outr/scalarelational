package org.scalarelational.datatype


class TypedValue[T, S](val dataType: DataType[T, S], val value: T) {
  override def equals(obj: Any): Boolean = obj match {
    case tv: TypedValue[_, _] => tv.value == value
    case _ => false
  }

  override def hashCode(): Int = value.hashCode()
}