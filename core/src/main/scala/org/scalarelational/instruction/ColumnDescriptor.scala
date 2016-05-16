package org.scalarelational.instruction

import org.scalarelational.column.params.ColumnParam
import org.scalarelational.column.types.ColumnType
import org.scalarelational.instruction.args.SQLArgument

case class ColumnDescriptor[T](name: String, columnType: ColumnType[T], params: List[ColumnParam]) extends SQLPart {
  override def sql: String = if (params.nonEmpty) {
    s"$name ${columnType.sql} ${params.map(_.sql).mkString(" ")}"
  } else {
    s"$name ${columnType.sql}"
  }

  override def args: Vector[SQLArgument] = mergeArgs(columnType :: params: _*)

  def withParam(param: ColumnParam): ColumnDescriptor[T] = copy(params = params ++ List(param))
}