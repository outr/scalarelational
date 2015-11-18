package org.scalarelational.extra

import org.powerscala.property.Property
import org.scalarelational.Session
import org.scalarelational.column.property.{AutoIncrement, PrimaryKey, Unique}
import org.scalarelational.model.Datastore
import org.scalarelational.table.Table

/**
 * Persistent Properties allows key/value pairs to be persisted to a table for later retrieval and modification.
 *
 * Convenience methods are provided to get, set, and remove the value for a property and to create a Property instance
 * that can update the database in an event-driven manner.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait PersistentProperties extends Datastore {
  object persistentProperties extends Table("PERSISTENT_PROPERTIES") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val key = column[String]("name", Unique)
    val value = column[String]("value")
  }

  object persistence {
    def get(name: String): Option[String] = withSession { implicit session =>
      val query = select(persistentProperties.value) from persistentProperties where persistentProperties.key === name
      query.converted.headOption
    }

    def apply(name: String): String = {
      get(name).getOrElse(throw new NullPointerException(s"Unable to find $name in persistent properties table."))
    }

    def update(name: String, newValue: String)(implicit session: Session): Unit = {
      val m = merge(
        persistentProperties.key,
        persistentProperties.key(name),
        persistentProperties.value(newValue))
      m.result
    }

    def remove(name: String)(implicit session: Session) {
      val d = delete (persistentProperties) where (persistentProperties.key === name)
      d.result
    }

    def stringProperty(key: String, default: String = null)
                      (implicit session: Session): Property[String] = {
      val p = Property[String](default = Some(get(key).getOrElse(default)))
      p.change.on {
        case evt => if (evt.newValue != null) {
          this(key) = evt.newValue
        } else {
          remove(key)
        }
      }
      p
    }

    def intProperty(key: String, default: Int = 0)
                   (implicit session: Session): Property[Int] = {
      val p = Property[Int](default = Some(get(key).map(s => s.toInt).getOrElse(default)))
      p.change.on {
        case evt => this(key) = evt.newValue.toString
      }
      p
    }
  }
}