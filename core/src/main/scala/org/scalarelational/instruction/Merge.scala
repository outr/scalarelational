package org.scalarelational.instruction

import org.scalarelational.Session
import org.scalarelational.column.{Column, ColumnValue}
import org.scalarelational.table.Table

import scala.language.existentials

case class Merge(table: Table, key: Column[_, _], values: List[ColumnValue[_, _]]) extends Instruction[Int] {
  def result(implicit session: Session): Int = if (table.datastore.supportsMerge) {
    table.datastore.exec(this)
  } else {    // Work-around for databases that don't support MERGE
    import table.datastore._
    val keyValue = values.find(cv => cv.column eq key).getOrElse(throw new RuntimeException(s"Unable to find value for ${key.name}"))
    val remove = delete(table) where key.asInstanceOf[Column[Any, Any]] === keyValue.value
    val create = insert(values: _*)
    remove.result
    create.result
  }
}