package org.scalarelational.mapper

import org.scalarelational.column.property.{NotNull, AutoIncrement, PrimaryKey}
import org.scalarelational.model.Table
import org.scalatest.{Matchers, WordSpec}

import org.scalarelational.h2.{H2Datastore, H2Memory}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class MapperSpec extends WordSpec with Matchers {
  "Mapper" when {
    import Datastore._

    "doing setup" should {
      "create the database" in {
        session {
          create(people)
        }
      }
      "insert some people into the database" in {
        import people._

        session {
          insert(name("John Doe"), age(21)).
             add(name("Jane Doe"), age(19)).
             add(name("Baby Doe"), age(2)).result
        }
      }
    }
    "dealing with queries" should {
      import people._

      "explicitly map to a case class" in {
        session {
          val query = select(*) from people where name === "John Doe"
          val john = query.mapped(qr => Person(qr(name), qr(age), qr(id))).head
          john should equal(Person("John Doe", 21, Some(1)))
        }
      }
      "explicitly map to a (Name, Age) type" in {
        session {
          val query = select(*) from people where name === "John Doe"
          val john = query.mapped(qr => (Name(qr(name)), Age(qr(age)))).head
          john should equal((Name("John Doe"), Age(21)))
        }
      }
      "automatically map to a case class" in {
        session {
          val query = select(*) from people where name === "Jane Doe"
          val jane = query.as[Person].head
          jane should equal(Person("Jane Doe", 19, Some(2)))
        }
      }
    }
    "dealing with inserts" should {
      "automatically convert a case class to an insert" in {
        session {
          people.persist(Person("Ray Doe", 30)).result
        }
      }
      "query back the inserted object" in {
        import people._

        session {
          val query = select(*) from people where name === "Ray Doe"
          val ray = query.as[Person].head
          ray should equal(Person("Ray Doe", 30, Some(4)))
        }
      }
      "automatically convert a case class to an update" in {
        session {
          people.persist(Person("Jay Doe", 30, Some(4))).result
        }
      }
      "query back the updated object" in {
        import people._

        session {
          val query1 = select(*) from people where name === "Ray Doe"
          query1.as[Person].headOption should equal(None)
          val query2 = select(*) from people where name === "Jay Doe"
          val jay = query2.as[Person].head
          jay should equal(Person("Jay Doe", 30, Some(4)))
        }
      }
    }
  }
}

case class Person(name: String, age: Int, id: Option[Int] = None)

case class Name(value: String)

case class Age(value: Int)

object Datastore extends H2Datastore(mode = H2Memory("mapper")) {
  object people extends Table("person") {
    val id = column[Option[Int]]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", NotNull)
    val age = column[Int]("age", NotNull)
  }
}