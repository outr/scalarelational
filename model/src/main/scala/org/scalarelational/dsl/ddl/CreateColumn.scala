package org.scalarelational.dsl.ddl

import org.scalarelational.{Database, Session}
import org.scalarelational.instruction.Instruction

case class CreateColumnEntry[T](name: String, columnType: String, attributes: List[ColumnAttribute[T]] = Nil) {
  def describe: String = {
    val b = new StringBuilder
    b.append(s"$name $columnType")
    b.append(attributes.map(a => s" ${a.describe}").mkString)
    b.toString()
  }

  def forTable[D <: Database](database: D, tableName: String): CreateColumn[D, T] = {
    CreateColumn(database, tableName, name, columnType, attributes)
  }

  def withAttribute(attribute: ColumnAttribute[T]): CreateColumnEntry[T] = {
    copy(attributes = attributes :+ attribute)
  }
}

case class CreateColumn[D <: Database, T](database: D,
                                          tableName: String,
                                          name: String,
                                          columnType: String,
                                          attributes: List[ColumnAttribute[T]] = Nil) extends Instruction[D, Boolean] {
  override def describe: String = {
    val b = new StringBuilder
    b.append(s"ALTER TABLE $tableName ADD $name $columnType")
    b.append(attributes.map(a => s" ${a.describe}").mkString)
    b.toString()
  }

  override def args: Vector[Any] = Vector.empty

  def withAttribute(attribute: ColumnAttribute[T]): CreateColumn[D, T] = {
    copy(attributes = attributes :+ attribute)
  }

  override def exec()(implicit session: Session[D]): Boolean = call(session)
}

trait ColumnAttribute[T] {
  def describe: String
}

object ColumnAttribute {
  def apply[T](value: String): ColumnAttribute[T] = new ColumnAttribute[T] {
    override def describe: String = value
  }
}