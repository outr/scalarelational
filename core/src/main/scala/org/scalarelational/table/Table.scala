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

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class Table(name: String, tableProperties: TableProperty*)
                    (implicit val datastore: Datastore)
  extends Joinable with SQLContainer with DataTypeSupport with TablePropertyContainer {

  lazy val tableName = if (name == null) Table.generateName(getClass) else name

  datastore.add(this)   // Make sure the Datastore knows about this table

  implicit def thisTable = this

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

  protected[scalarelational] def addColumn[T, S](column: Column[T, S]) = synchronized {
    _columns += column
  }

  def as(alias: String) = TableAlias(this, alias)

  def primaryKey: Column[_, _] = columns.find(_.has(PrimaryKey)).get

  def columns = _columns.toList

  def * = columns

  def getColumn[T, S](name: String) = columnMap.get(name.toLowerCase).asInstanceOf[Option[Column[T, S]]]
  def getColumnByField[T, S](name: String) = columnMap.values.find(c => c.fieldName == name).asInstanceOf[Option[Column[T, S]]]
  def columnsByName[T, S](names: String*) = names.flatMap(name => getColumn[T, S](name))

  def column[T](name: String, properties: ColumnProperty*)
               (implicit dataType: SimpleDataType[T], manifest: Manifest[T]): Column[T, T] =
    new Column[T, T](name, dt(dataType, properties, manifest), manifest, this, properties)

  def column[T](name: String, dataType: SimpleDataType[T], properties: ColumnProperty*)
               (implicit manifest: Manifest[T]): Column[T, T] =
    new Column[T, T](name, dt(dataType, properties, manifest), manifest, this, properties)

  def column[T, S](name: String, properties: ColumnProperty*)
               (implicit dataType: DataType[T, S], manifest: Manifest[T]): Column[T, S] =
    new Column[T, S](name, dt(dataType, properties, manifest), manifest, this, properties)

  def column[T, S](name: String, dataType: DataType[T, S], properties: ColumnProperty*)
                  (implicit manifest: Manifest[T]): Column[T, S] =
    new Column[T, S](name, dt(dataType, properties, manifest), manifest, this, properties)

  private def dt[T, S](dt: DataType[T, S], properties: Seq[ColumnProperty], manifest: Manifest[T]): DataType[T, S] = {
    val instance = DataTypeInstance[Any, Any](dt.asInstanceOf[DataType[Any, Any]], properties, manifest.asInstanceOf[Manifest[Any]])
    datastore.dataTypeInstanceProcessor.fire(instance).asInstanceOf[DataType[T, S]]
  }

  protected[scalarelational] def allFields(tpe: Class[_]): Seq[Field] = {
    tpe.getSuperclass match {
      case null => tpe.getDeclaredFields
      case s => tpe.getDeclaredFields ++ allFields(s)
    }
  }

  protected[scalarelational] def fieldName(column: Column[_, _]) = {
    allFields(getClass).find(f => {
      f.setAccessible(true)
      f.get(this) == column
    }).map(_.getName).getOrElse(throw new RuntimeException(s"Unable to find field name in '$tableName' for '${column.name}'."))
  }

  def exists = datastore.doesTableExist(tableName)

  override def toString = tableName
}

object Table {
  def generateName(c: Class[_]): String = {
    val n = c.getSimpleName
    "([A-Z])".r.replaceAllIn(
      n.charAt(0).toLower + n.substring(1, n.length - 1),
      m => "_" + m.group(0).toLowerCase)
  }
}
