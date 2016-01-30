package org.scalarelational.extra

import org.scalarelational.Session
import org.scalarelational.column.property.{AutoIncrement, PrimaryKey, Unique}
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
  object persistentProperties extends Table("PERSISTENT_PROPERTIES") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val key = column[String]("name", Unique)
    val value = column[String]("value")
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

    def update(name: String, newValue: String)
              (implicit session: Session): Unit =
      merge(
        persistentProperties.key,
        persistentProperties.key(name),
        persistentProperties.value(newValue)).result

    def remove(name: String)(implicit session: Session): Unit =
      (delete (persistentProperties)
        where (persistentProperties.key === name)).result

    def stringProperty(key: String)(implicit session: Session): Opt[String] = {
      val ch = new Opt[String](get(key))
      ch.silentAttach {
        case Some(value) => this(key) = value
        case None        => remove(key)
      }
      ch
    }

    def intProperty(key: String)(implicit session: Session): Opt[Int] = {
      val ch = new Opt[Int](get(key).map(_.toInt))
      ch.silentAttach {
        case Some(value) => this(key) = value.toString
        case None        => remove(key)
      }
      ch
    }

    def longProperty(key: String)(implicit session: Session): Opt[Long] = {
      val ch = new Opt[Long](get(key).map(_.toLong))
      ch.silentAttach {
        case Some(value) => this(key) = value.toString
        case None => remove(key)
      }
      ch
    }
  }
}