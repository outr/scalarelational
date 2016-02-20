package org.scalarelational.dsl.ddl

case class CreateColumn[T](name: String, columnType: String, attributes: List[ColumnAttribute[T]] = Nil) extends DDL {
  override def describe: String = {
    val b = new StringBuilder
    b.append(s"$name $columnType")
    b.append(attributes.map(a => s" ${a.describe}").mkString)
    b.toString()
  }

  def withAttribute(attribute: ColumnAttribute[T]): CreateColumn[T] = {
    copy(attributes = attributes :+ attribute)
  }
}

trait ColumnAttribute[T] extends DDL

object ColumnAttribute {
  def apply[T](value: String): ColumnAttribute[T] = new ColumnAttribute[T] {
    override def describe: String = value
  }
}