package com.outr.query

import scala.collection.mutable.ListBuffer
import com.outr.query.property.{ColumnProperty, AutoIncrement, ForeignKey, PrimaryKey}
import scala.language.existentials
import com.outr.query.convert._
import com.outr.query.table.property.{Linking, TableProperty}

/**
 * @author Matt Hicks <matt@outr.com>
 */
abstract class Table(val datastore: Datastore, name: String, tableProperties: TableProperty*) {
  def this(datastore: Datastore, tableProperties: TableProperty*) = this(datastore, null.asInstanceOf[String], tableProperties: _*)

  lazy val tableName = if (name == null) Table.generateName(getClass) else name
  datastore.add(this)   // Make sure the Datastore knows about this table

  implicit def thisTable = this

  implicit def booleanConverter = BooleanConverter
  implicit def intConverter = IntConverter
  implicit def longConverter = LongConverter
  implicit def doubleConverter = DoubleConverter
  implicit def bigDecimalConverter = BigDecimalConverter
  implicit def stringConverter = StringConverter
  implicit def byteArrayConverter = ByteArrayConverter
  implicit def blobConverter = BlobConverter

  private var _properties = Map.empty[String, TableProperty]
  private var _columns = ListBuffer.empty[Column[_]]
  private var _foreignColumns = ListBuffer.empty[Column[_]]
  private lazy val columnMap = Map(columns.map(c => c.name.toLowerCase -> c): _*)
  lazy val primaryKeys = columns.collect {
    case c if c.has(PrimaryKey) => c
  }
  lazy val foreignKeys = columns.collect {
    case c if c.has(ForeignKey.name) => c
  }
  lazy val autoIncrement = columns.find(c => c.has(AutoIncrement))
  lazy val (one2One, one2Many, many2One, many2Many) = loadRelationships()

  props(tableProperties: _*)      // Add properties from constructor

  def properties = _properties.values

  protected[query] def addColumn[T](column: Column[T]) = synchronized {
    _columns += column
  }
  protected[query] def addForeignColumn[T](column: Column[T]) = synchronized {
    _foreignColumns += column
  }

  def as(alias: String) = TableAlias(this, alias)

  def columns = _columns.toList

  def * = columns

  def getColumn[T](name: String) = columnMap.get(name.toLowerCase).asInstanceOf[Option[Column[T]]]
  def columnsByName[T](names: String*) = names.map(name => getColumn[T](name)).flatten

  def column[T](name: String, properties: ColumnProperty*)
               (implicit converter: ColumnConverter[T], manifest: Manifest[T]) = {
    val c = new Column[T](name, converter, manifest, this)
    c.props(properties: _*)
  }

  def column[T](name: String, converter: ColumnConverter[T], properties: ColumnProperty*)
               (implicit manifest: Manifest[T]) = {
    val c = new Column[T](name, converter, manifest, this)
    c.props(properties: _*)
  }

  private def loadRelationships() = {
    val local2Foreign = Map(columns.collect {
      case c if c.has(ForeignKey.name) => ForeignKey(c).foreignColumn.table -> c
    }: _*)
    val foreign2Local = Map(_foreignColumns.map(c => c.table -> c): _*)
    val foreignTables = local2Foreign.keySet ++ foreign2Local.keySet
    var o2o = List.empty[Column[_]]
    var o2m = List.empty[Column[_]]
    var m2o = List.empty[Column[_]]
    var m2m = List.empty[Column[_]]
    foreignTables.foreach {
      case foreignTable => if (local2Foreign.contains(foreignTable) && foreign2Local.contains(foreignTable)) {
        o2o = local2Foreign(foreignTable) :: o2o
      } else if (local2Foreign.contains(foreignTable)) {
        o2m = local2Foreign(foreignTable) :: o2m
      } else if (foreign2Local.contains(foreignTable)) {
        if (foreignTable.has(Linking)) {
          m2m = foreign2Local(foreignTable) :: m2m
        } else {
          m2o = foreign2Local(foreignTable) :: m2o
        }
      }
    }
    (o2o.reverse, o2m.reverse, m2o.reverse, m2m.reverse)
  }

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

  def has(property: TableProperty): Boolean = has(property.name)
  def has(propertyName: String): Boolean = _properties.contains(propertyName)
  def get[P <: TableProperty](propertyName: String) = _properties.collectFirst {
    case (key, property) if key == propertyName => property.asInstanceOf[P]
  }
  def prop[P <: TableProperty](propertyName: String) = _properties.get(propertyName).asInstanceOf[Option[P]]

  override def toString = s"Table($tableName)"
}

object Table {
  def generateName(c: Class[_]) = {
    val n = c.getSimpleName
    "([A-Z])".r.replaceAllIn(n.charAt(0).toLower + n.substring(1, n.length - 1), m => "_" + m.group(0).toLowerCase)
  }
}