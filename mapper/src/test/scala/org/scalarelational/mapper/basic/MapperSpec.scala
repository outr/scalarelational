package org.scalarelational.mapper.basic

import org.scalarelational.column.property._
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalarelational.mapper._
import org.scalarelational.model.Table
import org.scalatest.{Matchers, WordSpec}

/**
 * @author Matt Hicks <matt@outr.com>
 */
class MapperSpec extends WordSpec with Matchers {
  "Mapper" when {
    import Datastore._

    "doing setup" should {
      "create the database" in {
        session {
          create(people, suppliers, coffees)
        }
      }
      "insert some people into the database" in {
        import people._

        session {
          insert(name("John"), age(21), surname("Doe")).
             and(name("Jane"), age(19), surname("Doe")).result
          insert(name("Baby"), age(21)).result
        }
      }
    }
    "dealing with queries" should {
      import people._

      "explicitly map to a case class" in {
        session {
          val query = select(*) from people where name === "John"
          val john = query.convert[Person](qr => Person(qr(name), qr(age), Option(qr(surname)), Some(qr(id)))).result.one()
          john should equal(Person("John", 21, Some("Doe"), Some(1)))

          val query2 = select(*) from people where name === "Baby"
          val baby = query2.convert[Person](qr => Person(qr(name), qr(age), Option(qr(surname)), Some(qr(id)))).result.one()
          baby should equal(Person("Baby", 21, None, Some(3)))
        }
      }
      "explicitly map to a (Name, Age) type" in {
        session {
          val query = select(*) from people where name === "John"
          val john = query.convert[(Name, Age)](qr => (Name(qr(name)), Age(qr(age)))).result.head()
          john should equal((Name("John"), Age(21)))
        }
      }
      "automatically map to a case class" in {
        session {
          val query = select(*) from people where name === "Jane"
          val jane = query.to[Person].result.head()
          jane should equal(Person("Jane", 19, Some("Doe"), Some(2)))
        }
      }
      "map a joined query to two case classes" in {
        session {

        }
      }
    }
    "dealing with inserts" should {
      "automatically convert a case class to an insert" in {
        session {
          val ray = people.persist(Person("Ray", 30)).result
          ray should equal(Person("Ray", 30, None, Some(4)))
        }
      }
      "query back the inserted object" in {
        import people._

        session {
          val query = select(*) from people where name === "Ray"
          val ray = query.to[Person].result.head()
          ray should equal(Person("Ray", 30, None, Some(4)))
        }
      }
      "automatically convert a case class to an update" in {
        session {
          val jay = people.persist(Person("Jay", 30, None, Some(4))).result
          jay should equal(Person("Jay", 30, None, Some(4)))
        }
      }
      "query back the updated object" in {
        import people._

        session {
          val query1 = select(*) from people where name === "Ray"
          query1.to[Person].result.headOption should equal(None)
          val query2 = select(*) from people where name === "Jay"
          val jay = query2.to[Person].result.head()
          jay should equal(Person("Jay", 30, None, Some(4)))
        }
      }
    }
    "more complex relationships" should {
      "persist records" in {
        session {
          // Insert Suppliers
          val acme = suppliers.persist(Supplier("Acme, Inc.", "99 Market Street", "Groundsville", "CA", "95199")).result
          val superior = suppliers.persist(Supplier("Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460")).result
          val highGround = suppliers.persist(Supplier("The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966")).result

          // Insert Coffees
          coffees.persist(Coffee("Colombian", Some(acme.id.get), 7.99, 0, 0)).result
          coffees.persist(Coffee("French Roast", Some(superior.id.get), 8.99, 0, 0)).result
          coffees.persist(Coffee("Espresso", Some(highGround.id.get), 9.99, 0, 0)).result
          coffees.persist(Coffee("Colombian Decaf", Some(acme.id.get), 8.99, 0, 0)).result
          coffees.persist(Coffee("French Roast Decaf", Some(superior.id.get), 9.99, 0, 0)).result
          coffees.persist(Coffee("Caffè American", None, 12.99, 0, 0)).result
          // TODO: add batch insert / update support for persist
        }
      }
      "query back 'French Roast' with 'Superior Coffee'" in {
        session {
          val query = select(coffees.* ::: suppliers.*) from coffees innerJoin suppliers on(coffees.supId === suppliers.id) where(coffees.name === "French Roast")
          val (frenchRoast, superior) = query.to[Coffee, Supplier](coffees, suppliers).result.head()
          frenchRoast should equal(Coffee("French Roast", Some(superior.id.get), 8.99, 0, 0, Some(2)))
          superior should equal(Supplier("Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460", Some(2)))
        }
      }
      "query back 'Caffè American'" in {
        session {
          import coffees._
          val query = select(*) from coffees where name === "Caffè American"
          val caffe = query.to[Coffee].result.head()
          caffe should equal(Coffee("Caffè American", None, 12.99, 0, 0))
        }
      }
    }
  }
}

case class Person(name: String, age: Int, surname: Option[String] = None, id: Option[Int] = None)

case class Name(value: String)

case class Age(value: Int)

case class Supplier(name: String, street: String, city: String, state: String, zip: String, id: Option[Int] = None)

case class Coffee(name: String, supId: Option[Int], price: Double, sales: Int, total: Int, id: Option[Int] = None)

object Datastore extends H2Datastore(mode = H2Memory("mapper")) {
  object people extends Table("person") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name", NotNull)
    val age = column[Int]("age", NotNull)
    val surname = column[String]("surname")
  }
  object suppliers extends Table("SUPPLIERS") {
    val id = column[Int]("SUP_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("SUP_NAME")
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")
  }
  object coffees extends Table("COFFEES") {
    val id = column[Int]("COF_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("COF_NAME", Unique)
    val supId = column[Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")
  }
}