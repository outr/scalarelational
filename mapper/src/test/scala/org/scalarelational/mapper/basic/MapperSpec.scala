package org.scalarelational.mapper.basic

import org.scalarelational.column.property.{AutoIncrement, ForeignKey, PrimaryKey, Unique}
import org.scalarelational.datatype.Ref
import org.scalarelational.h2.{H2Datastore, H2Memory}
import org.scalarelational.mapper._
import org.scalatest.{Matchers, WordSpec}


class MapperSpec extends WordSpec with Matchers {
  "Mapper" when {
    import Datastore._

    "doing setup" should {
      "create the database" in {
        withSession { implicit session =>
          create(people, suppliers, coffees)
        }
      }
      "insert some people into the database" in {
        import people._

        withSession { implicit session =>
          insert(name("John"), age(21), surname(Some("Doe"))).
             and(name("Jane"), age(19), surname(Some("Doe"))).result
          insert(name("Baby"), age(21)).result
        }
      }
    }
    "dealing with queries" should {
      import people._

      "explicitly map to a case class" in {
        withSession { implicit session =>
          val query = select(*) from people where name === "John"
          val john = query.convert[Person](qr => Person(qr(name), qr(age), qr(surname), qr(id))).converted.one
          john should equal(Person("John", 21, Some("Doe"), Some(1)))

          val query2 = select(*) from people where name === "Baby"
          val baby = query2.convert[Person](qr => Person(qr(name), qr(age), qr(surname), qr(id))).converted.one
          baby should equal(Person("Baby", 21, None, Some(3)))
        }
      }
      "explicitly map to a (Name, Age) type" in {
        withSession { implicit session =>
          val query = select(*) from people where name === "John"
          val john = query.convert[(Name, Age)](qr => (Name(qr(name)), Age(qr(age)))).converted.head
          john should equal((Name("John"), Age(21)))
        }
      }
      "automatically map to a case class" in {
        withSession { implicit session =>
          val query = select(*) from people where name === "Jane"
          val jane = query.to[Person](people).converted.head
          jane should equal(Person("Jane", 19, Some("Doe"), Some(2)))
        }
      }
      "automatically map a subset of columns to a case class" in {
        withSession { implicit session =>
          val query = select(*) from people where name === "Jane"
          val jane = query.to[PartialPerson](people).converted.head
          jane should equal(PartialPerson("Jane", 19, Some(2)))
        }
      }
      "map a ResultSet via Macro" in {
        withSession { implicit session =>
          val query = select(*) from people where name === "Jane"
          val resultSet = query.result.rs
          resultSet.next()
          val converter = rsConverter[Person]
          val jane = converter(resultSet)
          jane should equal(Person("Jane", 19, Some("Doe"), Some(2)))
        }
      }
    }
    "dealing with inserts" should {
      "automatically convert a case class to an insert" in {
        withSession { implicit session =>
          val result = Person("Ray", 30).insert.result
          result.id should equal(4)
        }
      }
      "automatically convert a case class with a subset of optional columns to an insert" in {
        withSession { implicit session =>
          val result = PartialPerson("Ray2", 30).insert.result
          result.id should equal(5)
        }
      }
      // TODO: fix this to actually throw a compile-time exception instead of a pointless constraint violation
//      "don't convert a case class with missing non-optional columns to an insert" in {
//        withSession { implicit session =>
//          intercept[JdbcSQLException] {
//            PartialPersonWithoutAge("Ray3").insert.result
//          }
//        }
//      }
      "query back the inserted object" in {
        import people._

        withSession { implicit session =>
          val query = select(*) from people where name === "Ray"
          val ray = query.to[Person](people).converted.head
          ray should equal(Person("Ray", 30, None, Some(4)))
        }
      }
      "automatically convert a case class to an update" in {
        withSession { implicit session =>
          Person("Jay", 30, None, Some(4)).update.result
        }
      }
      "query back the updated object" in {
        import people._

        withSession { implicit session =>
          val query1 = select(*) from people where name === "Ray"
          query1.to[Person](people).result.headOption should equal(None)
          val query2 = select(*) from people where name === "Jay"
          val jay = query2.to[Person](people).converted.head
          jay should equal(Person("Jay", 30, None, Some(4)))
        }
      }
      "update limiting the fields" in {
        withSession { implicit session =>
          Person("Kay", 40, Some("Konnors"), Some(4)).update(people.name, people.surname).result
        }
      }
      "query back the limited updated object" in {
        withSession { implicit session =>
          val kay = (people.query where people.id === Some(4)).converted.head
          kay.name should equal("Kay")
          kay.age should equal(30)
          kay.surname should equal(Some("Konnors"))
        }
      }
      "query back names to make sure no data was corrupted" in {
        withSession { implicit session =>
          val query = select(people.name) from people
          val names = query.converted.toSet
          names should equal(Set("John", "Kay", "Jane", "Baby", "Ray2"))
        }
      }
    }
    "more complex relationships" should {
      "persist records" in {
        withSession { implicit session =>
          // Insert Suppliers
          val acmeId = Supplier("Acme, Inc.", "99 Market Street", "Groundsville", "CA", "95199").insert.result
          val superiorId = Supplier("Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460").insert.result
          val highGroundId = Supplier("The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966").insert.result

          // Insert Coffees
          Coffee("Colombian", Some(acmeId), 7.99, 0, 0).insert.
            and(Coffee("French Roast", Some(superiorId), 8.99, 0, 0).insert).
            and(Coffee("Espresso", Some(highGroundId), 9.99, 0, 0).insert).
            and(Coffee("Colombian Decaf", Some(acmeId), 8.99, 0, 0).insert).
            and(Coffee("French Roast Decaf", Some(superiorId), 9.99, 0, 0).insert).
            and(Coffee("Caffè American", None, 12.99, 0, 0).insert).result
        }
      }
      "query back 'French Roast' with 'Superior Coffee'" in {
        withSession { implicit session =>
          val query = select(coffees.* ::: suppliers.*) from coffees innerJoin suppliers on (coffees.supId === suppliers.ref.opt) where (coffees.name === "French Roast")
          val (frenchRoast, superior) = query.to[Coffee, Supplier](coffees, suppliers).converted.head
          frenchRoast should equal(Coffee("French Roast", Some(superior.ref), 8.99, 0, 0, Some(2)))
          superior should equal(Supplier("Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460", Some(2)))
        }
      }
      "query back 'Caffè American'" in {
        withSession { implicit session =>
          import coffees._
          val query = select (*) from coffees where name === "Caffè American"
          val caffe = query.to[Coffee](coffees).converted.head
          caffe should equal(Coffee("Caffè American", None, 12.99, 0, 0, Some(6)))
        }
      }
      "query back an item using MapTo" in {
        withSession { implicit session =>
          coffees.by(coffees.id, Some(1)) should equal(Some(Coffee("Colombian", Some(Ref[Supplier](1)), 7.99, 0, 0, Some(1))))
        }
      }
      "query back coffee and supplier into one case class" in {
        withSession { implicit session =>
          val query = (
            select
              (coffees.name as "name", coffees.price as "price", suppliers.name as "supplierName")
            from
              coffees
            innerJoin
              suppliers
            on
              coffees.supId === suppliers.ref.opt
            where
              coffees.name === "French Roast"
          ).loose[CoffeeAndSupplier]
          val entry = query.converted.head
          entry should be(CoffeeAndSupplier("French Roast", 8.99, "Superior Coffee"))
        }
      }
      "query multiple with left join" in {
        withSession { implicit session =>
          val query = (
            select
              (coffees.* ::: suppliers.*)
            from
              coffees
            leftJoin
              suppliers
            on
              coffees.supId === suppliers.ref.opt
            orderBy
              coffees.id.asc
          )
          val results = query.to[Coffee, Option[Supplier]](coffees, suppliers).converted.toVector
          results.length should equal(6)
          check(results.head, "Colombian", Some("Acme, Inc."))
          check(results(1), "French Roast", Some("Superior Coffee"))
          check(results(2), "Espresso", Some("The High Ground"))
          check(results(3), "Colombian Decaf", Some("Acme, Inc."))
          check(results(4), "French Roast Decaf", Some("Superior Coffee"))
          check(results(5), "Caffè American", None)

          def check(t: (Coffee, Option[Supplier]), coffeeName: String, supplierName: Option[String]): Unit = t match {
            case (coffee, supplier) => {
              coffee.name should equal(coffeeName)
              supplier.map(_.name) should equal(supplierName)
            }
          }
        }
      }
    }
    "working with @mapped Macro Annotation" should {
      val s = Supplier("Supplier Name", "Supplier Street", "Supplier City", "Supplier State", "Supplier Zip")

      "verify Supplier is an instance of TableMappable" in {
        s.isInstanceOf[Entity[_]] should equal(true)
      }
      "return exactly six ColumnValues" in {
        val values = s.columns
        values.length should equal(6)
      }
      "return the correct six ColumnValues" in {
        import suppliers._

        val values = s.columns
        values.head should equal(name("Supplier Name"))
        values.tail.head should equal(street("Supplier Street"))
        values.tail.tail.head should equal(city("Supplier City"))
        values.tail.tail.tail.head should equal(state("Supplier State"))
        values.tail.tail.tail.tail.head should equal(zip("Supplier Zip"))
        values.tail.tail.tail.tail.tail.head should equal(id(None))
      }
      "insert a @mapped Supplier" in {
        withSession { implicit session =>
          val target = Supplier("Target", "123 All Over Rd.", "Lotsaplaces", "California", "95461")
          target.insert.result.id should equal(4)
        }
      }
    }
    "deleting" should {
      import people._

      "find and delete Jane Doe" in {
        withSession { implicit session =>
          val query = select(*) from people where name === "Jane"
          val jane = query.to[Person](people).converted.head
          jane.delete.result
        }
      }
      "no longer find Jane Doe" in {
        withSession { implicit session =>
          val query = select(*) from people where name === "Jane"
          query.to[Person](people).converted.headOption should equal(None)
        }
      }
    }
  }
}

case class Person(name: String, age: Int, surname: Option[String] = None, id: Option[Int] = None) extends Entity[Person] {
  def columns = mapTo[Person](Datastore.people)

  object Test { // Objects are ignored by mapper
    val value = 42
  }
}

case class PartialPerson(name: String, age: Int, id: Option[Int] = None) extends Entity[PartialPerson] {
  def columns = mapTo[PartialPerson](Datastore.people)
}

case class PartialPersonWithoutAge(name: String, id: Option[Int] = None) extends Entity[PartialPersonWithoutAge] {
  def columns = mapTo[PartialPersonWithoutAge](Datastore.people)
}

case class Name(value: String)

case class Age(value: Int)

case class Supplier(name: String, street: String, city: String, state: String, zip: String, id: Option[Int] = None) extends Entity[Supplier] {
  def columns = mapTo[Supplier](Datastore.suppliers)
}

case class Coffee(name: String, supId: Option[Ref[Supplier]], price: Double, sales: Int, total: Int, id: Option[Int] = None) extends Entity[Coffee] {
  def columns = mapTo[Coffee](Datastore.coffees)
}

case class CoffeeAndSupplier(name: String, price: Double, supplierName: String)

object Datastore extends H2Datastore(mode = H2Memory("mapper")) {
  object people extends MappedTable[Person]("person") {
    val id = column[Option[Int], Int]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val age = column[Int]("age")
    val surname = column[Option[String], String]("surname")

    override def query = q.to[Person](this)
  }

  object suppliers extends MappedTable[Supplier]("SUPPLIERS") {
    val id = column[Option[Int], Int]("SUP_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("SUP_NAME")
    val street = column[String]("STREET")
    val city = column[String]("CITY")
    val state = column[String]("STATE")
    val zip = column[String]("ZIP")

    override def query = q.to[Supplier](this)
  }

  object coffees extends MappedTable[Coffee]("COFFEES") {
    val id = column[Option[Int], Int]("COF_ID", PrimaryKey, AutoIncrement)
    val name = column[String]("COF_NAME", Unique)
    val supId = column[Option[Ref[Supplier]], Int]("SUP_ID", new ForeignKey(suppliers.id))
    val price = column[Double]("PRICE")
    val sales = column[Int]("SALES")
    val total = column[Int]("TOTAL")

    override def query = q.to[Coffee](this)
  }
}