package org.scalarelational.datatype

import org.scalarelational.column.ColumnPropertyContainer
import org.scalarelational.column.property.{ColumnLength, IgnoreCase}
import org.scalarelational.model.Datastore


trait SQLType {
  def apply(datastore: Datastore, properties: ColumnPropertyContainer): String
}

object SQLType {
  def apply(value: String): SimpleSQLType = new SimpleSQLType(value)
}

class SimpleSQLType(value: String) extends SQLType {
  def apply(datastore: Datastore, properties: ColumnPropertyContainer): String = value
}

object StringSQLType extends SQLType {
  override def apply(datastore: Datastore, properties: ColumnPropertyContainer): String = {
    val length = properties.get[ColumnLength](ColumnLength.Name).map(_.length)
      .getOrElse(datastore.DefaultVarCharLength)
    if (properties.has(IgnoreCase)) {
      s"VARCHAR_IGNORECASE($length)"
    } else {
      s"VARCHAR($length)"
    }
  }
}

class BlobSQLType(value: String) extends SimpleSQLType(value)
