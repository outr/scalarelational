package org.scalarelational.table

import java.lang.reflect.Field

import org.scalarelational.column.Column
import org.scalarelational.column.property.{AutoIncrement, ColumnProperty, ForeignKey, PrimaryKey}
import org.scalarelational.datatype._
import org.scalarelational.instruction.Joinable
import org.scalarelational.model.{DataTypeInstance, Datastore, SQLContainer}
import org.scalarelational.table.property.TableProperty

import scala.collection.mutable.ListBuffer
import scala.language.existentials


abstract class Table(val tableName: String, tableProperties: TableProperty*)
                    (implicit val datastore: Datastore)
  extends Joinable with SQLContainer with DataTypeSupport with TablePropertyContainer {

  datastore.add(this)   // Make sure the Datastore knows about this table

  implicit def thisTable: Table = this

  private var _columns = ListBuffer.empty[Column[_, _]]
  private lazy val columnMap = Map(columns.map(c => c.name.toLowerCase -> c): _*)
  lazy val primaryKeys = columns.collect {
    case c if c.has(PrimaryKey) => c
  }
  lazy val foreignKeys = columns.collect {
    case c if c.has(ForeignKey.name) => c
  }
  lazy val autoIncrement = columns.find(c => c.has(AutoIncrement))

  lazy val q = datastore.select(*) from this

  props(tableProperties: _*)      // Add properties from constructor

  private def add[T, S](column: Column[T, S]): Column[T, S] = synchronized {
    _columns += column
    column
  }

  def as(alias: String): TableAlias = TableAlias(this, alias)

  def primaryKey: Column[_, _] = columns.find(_.has(PrimaryKey)).get

  def columns: List[Column[_, _]] = _columns.toList

  def `*`: List[Column[_, _]] = columns

  def getColumn[T, S](name: String): Option[Column[T, S]] = columnMap.get(name.toLowerCase).asInstanceOf[Option[Column[T, S]]]
  def getColumnByField[T, S](name: String): Option[Column[T, S]] = columnMap.values.find(c => c.fieldName == name).asInstanceOf[Option[Column[T, S]]]
  def columnsByName[T, S](names: String*): Seq[Column[T, S]] = names.flatMap(name => getColumn[T, S](name))

  def column[T](name: String, properties: ColumnProperty*)
               (implicit dataType: SimpleDataType[T]): Column[T, T] =
    add(new Column[T, T](name, dt(dataType, properties), this, properties))

  def column[T](name: String, dataType: SimpleDataType[T], properties: ColumnProperty*): Column[T, T] =
    add(new Column[T, T](name, dt(dataType, properties), this, properties))

  def column[T, S](name: String, properties: ColumnProperty*)
               (implicit dataType: DataType[T, S]): Column[T, S] =
    add(new Column[T, S](name, dt(dataType, properties), this, properties))

  def column[T, S](name: String, dataType: DataType[T, S], properties: ColumnProperty*): Column[T, S] =
    add(new Column[T, S](name, dt(dataType, properties), this, properties))

  private def dt[T, S](dt: DataType[T, S], properties: Seq[ColumnProperty]): DataType[T, S] = {
    val instance = DataTypeInstance[T, S](dt.asInstanceOf[DataType[T, S]], properties)
    datastore.dataTypeForInstance[T, S](instance)
  }

  protected[scalarelational] def allFields(tpe: Class[_]): Seq[Field] = tpe.getSuperclass match {
    case s: Class[_] => tpe.getDeclaredFields ++ allFields(s)
    case _ => tpe.getDeclaredFields
  }

  protected[scalarelational] def fieldName(column: Column[_, _]) = {
    allFields(getClass).find(f => {
      f.setAccessible(true)
      f.get(this) == column
    }).map(_.getName).getOrElse(throw new RuntimeException(s"Unable to find field name in '$tableName' for '${column.name}'."))
  }

  override def toString: String = tableName
}

object Table {
  def generateName(c: Class[_]): String = {
    val n = c.getSimpleName
    "([A-Z])".r.replaceAllIn(
      n.charAt(0).toLower + n.substring(1, n.length - 1),
      m => "_" + m.group(0).toLowerCase)
  }
}
