package org.scalarelational.instruction.ddl

import org.scalarelational.CallableInstruction
import org.scalarelational.column.property._
import org.scalarelational.model.ColumnPropertyContainer

import scala.collection.mutable.ListBuffer

/**
 * @author Matt Hicks <matt@outr.com>
 */
class BasicDDLSupport extends DDLSupport {
  override def ddl(create: CreateTable): List[CallableInstruction] = {
    val b = new StringBuilder

    b.append("CREATE TABLE IF NOT EXISTS ")
    b.append(create.name)
    b.append('(')
    b.append(create.columns.map(columnSQL).mkString(", "))

    val primaryKeys = create.columns.collect {
      case cc if cc.has(PrimaryKey) => cc
    }
    if (primaryKeys.nonEmpty) {
      b.append(s", PRIMARY KEY(${primaryKeys.map(c => c.name).mkString(", ")})")
    }

    b.append(");")

    List(CallableInstruction(b.toString()))
  }

  override def ddl[T](create: CreateColumn[T]): List[CallableInstruction] = {
    val b = new StringBuilder
    b.append(s"ALTER TABLE ${create.tableName} ADD ${columnSQL(create)}")
    List(CallableInstruction(b.toString()))
  }

  protected def columnSQL(create: CreateColumn[_]) = {
    val b = new StringBuilder
    b.append(create.name)
    b.append(' ')
    b.append(create.dataType.sqlType(create))
    val props = columnPropertiesSQL(create)
    if (props.nonEmpty) {
      b.append(props.mkString(" ", " ", ""))
    }
    b.toString()
  }

  protected def columnPropertiesSQL(container: ColumnPropertyContainer): List[String] = {
    val b = ListBuffer.empty[String]
    if (!container.isOptional && !container.has(Polymorphic)) {
      b.append("NOT NULL")
    }
    if (container.has(AutoIncrement)) {
      b.append("AUTO_INCREMENT")
    }
    if (container.has(Unique)) {
      b.append("UNIQUE")
    }
    b.toList
  }
}