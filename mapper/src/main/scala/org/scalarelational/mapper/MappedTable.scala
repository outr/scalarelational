package org.scalarelational.mapper

import org.scalarelational.datatype._
import org.scalarelational.model.Datastore
import org.scalarelational.instruction.{InsertSingle, Update}
import org.scalarelational.table.Table
import org.scalarelational.table.property.TableProperty
import org.scalarelational.column.{ColumnValue, RefColumn, ColumnLike, Column}
import org.scalarelational.column.property.PrimaryKey

abstract class MappedTable[MappedType](name: String, tableProperties: TableProperty*)
                                      (implicit val ds: Datastore)
  extends Table(name, tableProperties: _*)(ds) {
  def ref: ColumnLike[Ref[MappedType]] = RefColumn[MappedType](primaryKey)

  def query(implicit manifest: Manifest[MappedType]) = q.to[MappedType](manifest)

  def by[T](column: Column[T], value: T)
           (implicit manifest: Manifest[MappedType]) = datastore.session {
    val q = query where column === value
    q.result.converted.headOption
  }

  private[scalarelational] def updateColumnValues(values: List[ColumnValue[Any]]): Update[Ref[MappedType]] = {
    val primaryKey = values.find(_.column.has(PrimaryKey))
      .getOrElse(throw new RuntimeException("Update must have a PrimaryKey value specified to be able to update."))
    val primaryColumn = primaryKey.column
    val update = Update[Ref[MappedType]](this, values, null, Ref[MappedType])
    update where primaryColumn === primaryKey.value
  }

  private[scalarelational] def insertColumnValues(values: List[ColumnValue[Any]]): InsertSingle[Ref[MappedType]] =
    InsertSingle[Ref[MappedType]](this, values, Ref[MappedType])
}
