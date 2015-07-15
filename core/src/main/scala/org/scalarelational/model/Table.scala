package org.scalarelational.model

import java.lang.reflect.Field

import org.scalarelational.column.property.{AutoIncrement, ColumnProperty, ForeignKey, PrimaryKey}
import org.scalarelational.datatype._
import org.scalarelational.instruction.Joinable
import org.scalarelational.model.table.property.TableProperty
import org.scalarelational.TableAlias

import scala.collection.mutable.ListBuffer
import scala.language.existentials

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class Table(name: String, tableProperties: TableProperty*)
                    (implicit val datastore: Datastore)
  extends Joinable with SQLContainer with DataTypes with TablePropertyContainer {

  lazy val tableName = if (name == null) Table.generateName(getClass) else name

  datastore.add(this)   // Make sure the Datastore knows about this table

  implicit def thisTable = this

  private var _properties = Map.empty[String, TableProperty]
  private var _columns = ListBuffer.empty[Column[_]]
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

  def properties = _properties

  protected[scalarelational] def addColumn[T](column: Column[T]) = synchronized {
    _columns += column
  }

  def as(alias: String) = TableAlias(this, alias)

  def columns = _columns.toList

  def * = columns

  def getColumn[T](name: String) = columnMap.get(name.toLowerCase).asInstanceOf[Option[Column[T]]]
  def getColumnByField[T](name: String) = columnMap.values.find(c => c.fieldName == name).asInstanceOf[Option[Column[T]]]
  def columnsByName[T](names: String*) = names.flatMap(name => getColumn[T](name))

  def column[T](name: String, properties: ColumnProperty*)
               (implicit converter: DataType[T], manifest: Manifest[T]): Column[T] =
    new Column[T](name, converter, manifest, this, properties)

  def column[T](name: String, converter: DataType[T], properties: ColumnProperty*)
               (implicit manifest: Manifest[T]): Column[T] =
    new Column[T](name, converter, manifest, this, properties)

  protected[model] def allFields(tpe: Class[_]): Seq[Field] = {
    tpe.getSuperclass match {
      case null => tpe.getDeclaredFields
      case s => tpe.getDeclaredFields ++ allFields(s)
    }
  }

  protected[model] def fieldName(column: Column[_]) = {
    allFields(getClass).find(f => {
      f.setAccessible(true)
      f.get(this) == column
    }).map(_.getName).getOrElse(throw new RuntimeException(s"Unable to find field name in '$tableName' for '${column.name}'."))
  }

  def exists = datastore.doesTableExist(tableName)

  /**
   * Adds the supplied properties to this table.
   *
   * @param properties the properties to add
   * @return this
   */
  def props(properties: TableProperty*) = synchronized {
    properties.foreach {
      case p => {
        _properties += p.name -> p
        p.addedTo(this)
      }
    }
    this
  }

  override def toString = tableName
}

object Table {
  def generateName(c: Class[_]) = {
    val n = c.getSimpleName
    "([A-Z])".r.replaceAllIn(n.charAt(0).toLower + n.substring(1, n.length - 1), m => "_" + m.group(0).toLowerCase)
  }
}