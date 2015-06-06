package org.scalarelational.mapper

import org.scalarelational.column.property.{NotNull, AutoIncrement, PrimaryKey}
import org.scalarelational.model.Table
import org.scalatest.{Matchers, WordSpec}

import org.scalarelational.h2.{H2Datastore, H2Memory}

import org.scalarelational.dsl._

/**
 * @author Matt Hicks <matt@outr.com>
 */
class MapperSpec extends WordSpec with Matchers {
  "Mapper" when {
    import Datastore._

    "doing setup" should {
      "create the database" in {
        session {
          create(person)
        }
      }
      "insert some people into the database" in {
        import person._

        session {
          insert(name("John Doe"), age(21)).
             add(name("Jane Doe"), age(19)).
             add(name("Baby Doe"), age(2)).result
        }
      }
    }
    "dealing with queries" should {
      import person._

      "explicitly map to a case class" in {
        session {
          val query = select(*) from person where name === "John Doe"
          val john = query.mapped(qr => Person(qr(name), qr(age), Option(qr(id)))).head
          john should equal(Person("John Doe", 21, Some(1)))
        }
      }
      "automatically map to a case class" in {
        session {
          val query = select(*) from person where name === "Jane Doe"
          val jane = query.as[Person].head
          jane should equal(Person("Jane Doe", 19, Some(2)))
        }
      }
      // TODO: Test mapping to (Name, Age) tuple
    }
    "dealing with inserts" should {
      import person._

      // TODO: Test inserting a Person with explicit mapping
      // TODO: Test inserting a Person with auto mapping
      // TODO: Test inserting a (Name, Age) tuple
    }
  }
}

case class Person(name: String, age: Int, id: Option[Int] = None)

object Datastore extends H2Datastore(mode = H2Memory("mapper")) {
  object person extends Table("person") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", NotNull)
    val age = column[Int]("age", NotNull)
  }
}