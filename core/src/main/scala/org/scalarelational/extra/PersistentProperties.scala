package org.scalarelational.extra

import org.scalarelational.column.property.{AutoIncrement, ColumnLength, PrimaryKey, Unique}
import org.scalarelational.model.Datastore
import org.scalarelational.table.Table
import pl.metastack.metarx.Opt

/**
 * Persistent Properties allows key/value pairs to be persisted to a table for
 * later retrieval and modification.
 *
 * Convenience methods are provided to get, set, and remove the value for a
 * property and to create a Property instance that can update the database in an
 * event-driven manner.
 */
trait PersistentProperties extends Datastore {
  protected val PersistentKeyLength = 120
  protected val PersistentValueLength = 120

  object persistentProperties extends Table("PERSISTENT_PROPERTIES") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val key = column[String]("name", Unique, ColumnLength(PersistentKeyLength))
    val value = column[String]("value", ColumnLength(PersistentValueLength))
  }

  object persistence {
    def get(name: String): Option[String] = withSession { implicit session =>
      val query = (select (persistentProperties.value)
        from persistentProperties
        where persistentProperties.key === name
      )

      query.converted.headOption
    }

    def apply(name: String): String =
      get(name).getOrElse(
        throw new NullPointerException(s"Unable to find $name in persistent properties table."))

    def update(name: String, newValue: String): Unit = withSession { implicit session =>
      merge(
        persistentProperties.key,
        persistentProperties.key(name),
        persistentProperties.value(newValue)).result
    }

    def remove(name: String): Unit = withSession { implicit session =>
      (delete(persistentProperties)
        where (persistentProperties.key === name)).result
    }

    def stringProperty(key: String): Opt[String] = {
      val ch = new Opt[String](get(key))
      ch.silentAttach {
        case Some(value) => this(key) = value
        case None => remove(key)
      }
      ch
    }

    def intProperty(key: String): Opt[Int] = {
      val ch = new Opt[Int](get(key).map(_.toInt))
      ch.silentAttach {
        case Some(value) => this(key) = value.toString
        case None => remove(key)
      }
      ch
    }

    def longProperty(key: String): Opt[Long] = {
      val ch = new Opt[Long](get(key).map(_.toLong))
      ch.silentAttach {
        case Some(value) => this(key) = value.toString
        case None => remove(key)
      }
      ch
    }
  }
}